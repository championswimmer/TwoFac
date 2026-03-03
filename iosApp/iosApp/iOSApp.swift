import SwiftUI

@main
struct iOSApp: App {
    private func loadRocketSimConnect() {
        #if DEBUG
        guard (Bundle(path: "/Applications/RocketSim.app/Contents/Frameworks/RocketSimConnectLinker.nocache.framework")?.load() == true) else {
            print("Failed to load linker framework")
            return
        }
        print("RocketSim Connect successfully linked")
        #endif
    }
    init() {
        loadRocketSimConnect()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
