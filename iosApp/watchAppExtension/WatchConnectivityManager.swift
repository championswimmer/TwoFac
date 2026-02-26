import Foundation
import WatchConnectivity
import TwoFacKit
import SwiftUI

class WatchConnectivityManager: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WatchConnectivityManager()
    
    @Published var accounts: [WatchSyncAccount] = []
    @Published var lastSyncTime: Date? = nil
    
    override init() {
        super.init()
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = self
            session.activate()
        }
    }
    
    private func processPayloadData(_ payloadString: String) {
        do {
            let snapshot = try IosWatchSyncHelper.shared.decodeWatchSyncPayloadString(payload: payloadString)
            
            DispatchQueue.main.async {
                self.accounts = snapshot.accounts
                self.lastSyncTime = Date(timeIntervalSince1970: TimeInterval(snapshot.generatedAtEpochSec))
                print("Successfully processed sync payload. Loaded \(self.accounts.count) accounts.")
            }
        } catch {
            print("Failed to decode watch sync payload: \(error)")
        }
    }
    
    // MARK: - WCSessionDelegate
    
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if let error = error {
            print("WCSession activation failed: \(error.localizedDescription)")
            return
        }
        print("WCSession activated on watch.")
        
        // Check if there's an existing context we should process
        let context = session.receivedApplicationContext
        if let payloadString = context["payloadString"] as? String {
            processPayloadData(payloadString)
        }
    }
    
    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String : Any]) {
        print("Received application context on watch.")
        if let payloadString = applicationContext["payloadString"] as? String {
            processPayloadData(payloadString)
        }
    }
    
    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String : Any] = [:]) {
        print("Received user info on watch.")
        if let payloadString = userInfo["payloadString"] as? String {
            processPayloadData(payloadString)
        }
    }
}
