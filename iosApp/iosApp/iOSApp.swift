import SwiftUI
import WatchConnectivity

@main
struct iOSApp: App {
    
    @StateObject private var watchSyncCoordinator = WatchSyncCoordinator.shared
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}