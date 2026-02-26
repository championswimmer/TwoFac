import SwiftUI
import WatchConnectivity

class WatchSessionDelegate: NSObject, WCSessionDelegate {
    static let shared = WatchSessionDelegate()

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        // Log activation result so WCSession activation issues are diagnosable
        print("WCSession activation did complete with state: \(activationState.rawValue)")

        if let error = error {
            print("WCSession activation failed with error: \(error.localizedDescription)")
        }
    }
}

@main
struct TwoFacWatchApp: App {
    init() {
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = WatchSessionDelegate.shared
            session.activate()
        }
    }

    var body: some Scene {
        WindowGroup {
            WatchExtensionContentView()
        }
    }
}
