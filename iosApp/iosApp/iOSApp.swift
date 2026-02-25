import SwiftUI
import WatchConnectivity

class PhoneSessionDelegate: NSObject, WCSessionDelegate {
    static let shared = PhoneSessionDelegate()

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
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