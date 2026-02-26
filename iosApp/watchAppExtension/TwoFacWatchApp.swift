import SwiftUI

@main
struct TwoFacWatchApp: App {
    @StateObject private var connectivityManager = WatchConnectivityManager.shared

    var body: some Scene {
        WindowGroup {
            WatchExtensionContentView()
                .environmentObject(connectivityManager)
        }
    }
}
