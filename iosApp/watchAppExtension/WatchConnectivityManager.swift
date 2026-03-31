import Foundation
import WatchConnectivity
import TwoFacKit
import SwiftUI

class WatchConnectivityManager: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WatchConnectivityManager()
    
    @Published var accounts: [WatchSyncAccount] = []
    @Published var lastSyncTime: Date? = nil
    @Published var lastSyncError: String? = nil
    @Published var debugEvents: [String] = []

    private let payloadStringKey = WatchSyncContract.shared.IOS_WC_PAYLOAD_STRING_KEY
    private var latestAppliedGeneratedAtEpochSec: Int64 = 0
    
    override init() {
        super.init()
        log("Manager init")
        refreshFromPersistedPayload()

        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = self
            session.activate()
            log("WCSession activate requested")
        } else {
            log("WCSession not supported on this device")
        }
    }
    
    func refreshFromLatestAvailableData() {
        log("refreshFromLatestAvailableData invoked")
        if WCSession.isSupported() {
            let session = WCSession.default
            let context = session.receivedApplicationContext
            log(
                "Session state=\(session.activationState.rawValue) " +
                "reachable=\(session.isReachable) contextKeys=\(context.keys.sorted())"
            )
            if let payloadString = context[payloadStringKey] as? String {
                log("Found payload in receivedApplicationContext (length=\(payloadString.count))")
                persistPayloadString(payloadString)
                processPayloadData(payloadString, source: "receivedApplicationContext")
                return
            }
            log("No payloadString key in receivedApplicationContext")
        }
        refreshFromPersistedPayload()
    }

    private func processPayloadData(_ payloadString: String, source: String) {
        log("Processing payload from \(source) (length=\(payloadString.count))")
        do {
            let snapshot = try IosWatchSyncHelper.shared.decodeWatchSyncPayloadString(payload: payloadString)
            let generatedAtEpochSec = snapshot.generatedAtEpochSec

            if generatedAtEpochSec < latestAppliedGeneratedAtEpochSec {
                log(
                    "Ignoring stale payload from \(source). " +
                    "generatedAt=\(generatedAtEpochSec) latest=\(latestAppliedGeneratedAtEpochSec)"
                )
                return
            }
            latestAppliedGeneratedAtEpochSec = generatedAtEpochSec
            
            DispatchQueue.main.async {
                self.accounts = snapshot.accounts
                self.lastSyncTime = Date(timeIntervalSince1970: TimeInterval(snapshot.generatedAtEpochSec))
                self.lastSyncError = nil
                self.log(
                    "Processed payload from \(source). " +
                    "generatedAt=\(snapshot.generatedAtEpochSec) accounts=\(self.accounts.count)"
                )
            }
        } catch {
            DispatchQueue.main.async {
                self.lastSyncError = "Failed to decode sync payload"
            }
            log("Failed to decode payload from \(source): \(error.localizedDescription)")
        }
    }

    private func handleIncomingPayloadString(_ payloadString: String, source: String) {
        log("Incoming payload from \(source) (length=\(payloadString.count))")
        persistPayloadString(payloadString)
        // Re-read the persisted payload to guarantee UI reflects exactly what was saved.
        refreshFromPersistedPayload()
        log("Applied payload from \(source)")
    }

    private func refreshFromPersistedPayload() {
        guard let payloadString = loadPersistedPayloadString() else {
            log("No persisted payload found")
            return
        }
        log("Loaded persisted payload (length=\(payloadString.count))")
        processPayloadData(payloadString, source: "persistedPayload")
    }

    private func persistedPayloadFileURL() -> URL? {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?
            .appendingPathComponent("watch-sync-payload.json")
    }

    private func persistPayloadString(_ payloadString: String) {
        guard let fileURL = persistedPayloadFileURL() else { return }
        do {
            try payloadString.write(to: fileURL, atomically: true, encoding: .utf8)
            log("Persisted payload to \(fileURL.lastPathComponent)")
        } catch {
            log("Failed to persist payload: \(error.localizedDescription)")
        }
    }

    private func loadPersistedPayloadString() -> String? {
        guard let fileURL = persistedPayloadFileURL(),
              FileManager.default.fileExists(atPath: fileURL.path) else {
            return nil
        }
        return try? String(contentsOf: fileURL, encoding: .utf8)
    }
    
    // MARK: - WCSessionDelegate
    
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if let error = error {
            log("WCSession activation failed: \(error.localizedDescription)")
            return
        }
        log(
            "WCSession activated. state=\(activationState.rawValue) " +
            "reachable=\(session.isReachable)"
        )
        
        // Check if there's an existing context we should process
        let context = session.receivedApplicationContext
        log("Activation context keys=\(context.keys.sorted())")
        if let payloadString = context[payloadStringKey] as? String {
            handleIncomingPayloadString(payloadString, source: "activationContext")
        } else {
            refreshFromPersistedPayload()
        }
    }
    
    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String : Any]) {
        log("didReceiveApplicationContext keys=\(applicationContext.keys.sorted())")
        if let payloadString = applicationContext[payloadStringKey] as? String {
            handleIncomingPayloadString(payloadString, source: "applicationContext")
        } else {
            log("applicationContext missing payloadString key")
        }
    }
    
    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String : Any] = [:]) {
        log("didReceiveUserInfo keys=\(userInfo.keys.sorted())")
        if let payloadString = userInfo[payloadStringKey] as? String {
            handleIncomingPayloadString(payloadString, source: "userInfo")
        } else {
            log("userInfo missing payloadString key")
        }
    }

    func session(_ session: WCSession, didReceiveMessage message: [String : Any]) {
        log("didReceiveMessage keys=\(message.keys.sorted())")
        if let payloadString = message[payloadStringKey] as? String {
            handleIncomingPayloadString(payloadString, source: "message")
        } else {
            log("message missing payloadString key")
        }
    }

    func otpCode(for account: WatchSyncAccount) -> String {
        let uri = account.otpAuthUri
        if uri.lowercased().hasPrefix("otpauth://hotp") {
            return LibtwofacKt.genHOTPFromUri(otpauthUri: uri)
        }
        return LibtwofacKt.genTOTPFromUri(otpauthUri: uri)
    }

    private func log(_ message: String) {
        let timestamp = ISO8601DateFormatter().string(from: Date())
        let line = "\(timestamp) \(message)"
        print("[WatchSync] \(line)")
        DispatchQueue.main.async {
            self.debugEvents.append(line)
            if self.debugEvents.count > 200 {
                self.debugEvents.removeFirst(self.debugEvents.count - 200)
            }
        }
    }
}
