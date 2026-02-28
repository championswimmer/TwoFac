import Foundation
import SwiftUI
import WatchConnectivity
import TwoFacUIKit

class WatchSyncCoordinator: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WatchSyncCoordinator()

    @Published var isSupported: Bool = WCSession.isSupported()
    @Published var isReachable: Bool = false

    private let twoFacLib: TwoFacLib

    init(twoFacLib: TwoFacLib = TwoFacLib.Companion().initialise(storage: MemoryStorage(), passKey: nil)) {
        self.twoFacLib = twoFacLib
        super.init()
        if isSupported {
            let session = WCSession.default
            session.delegate = self
            session.activate()
        }
    }

    func syncNow() {
        guard isSupported, WCSession.default.activationState == .activated else {
            print("WCSession not activated")
            return
        }

        Task {
            do {
                guard let payloadString = try await IosWatchSyncHelper.shared.getWatchSyncPayloadString(lib: twoFacLib) else {
                    print("No payload generated (perhaps lib is locked or no accounts available)")
                    return
                }

                // Using updateApplicationContext for immediate state sync
                let context: [String: Any] = [
                    "payloadString": payloadString,
                    "generatedAtEpochSec": Date().timeIntervalSince1970
                ]

                try WCSession.default.updateApplicationContext(context)
                print("Successfully updated application context with payload.")

            } catch {
                print("Failed to sync to watch: \(error)")
            }
        }
    }

    // MARK: - WCSessionDelegate

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        DispatchQueue.main.async {
            self.isReachable = session.isReachable
        }
        if let error = error {
            print("WCSession activation failed with state \(activationState) error: \(error.localizedDescription)")
        } else {
            print("WCSession activation completed with state \(activationState)")
            // Potentially trigger a sync if needed
        }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {
        print("WCSession became inactive")
    }

    func sessionDidDeactivate(_ session: WCSession) {
        print("WCSession deactivated")
        // Required to support multiple watches
        WCSession.default.activate()
    }

    func sessionReachabilityDidChange(_ session: WCSession) {
        DispatchQueue.main.async {
            self.isReachable = session.isReachable
        }
        print("WCSession reachability changed: \(session.isReachable)")
    }
}
