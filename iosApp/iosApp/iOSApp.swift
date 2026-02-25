import SwiftUI
import WatchConnectivity

class PhoneSessionDelegate: NSObject, WCSessionDelegate {
    static let shared = PhoneSessionDelegate()

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if let error = error {
            print("WCSession activation failed with state \(activationState) and error: \(error.localizedDescription)")
        } else {
            print("WCSession activation completed with state \(activationState) and no error.")
        }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {
    }

    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
}

@main
struct iOSApp: App {
    init() {
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = PhoneSessionDelegate.shared
            session.activate()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}