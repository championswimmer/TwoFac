import SwiftUI
import TwoFacKit

struct WatchExtensionContentView: View {
    @EnvironmentObject private var connectivityManager: WatchConnectivityManager
    @Environment(\.scenePhase) private var scenePhase

    private func remainingSeconds(for date: Date) -> Int {
        let seconds = Int(date.timeIntervalSince1970)
        let mod = seconds % 30
        return mod == 0 ? 30 : 30 - mod
    }

    private func elapsedProgress(for date: Date) -> Double {
        let elapsedInWindow = date.timeIntervalSince1970.truncatingRemainder(dividingBy: 30.0)
        return min(max(elapsedInWindow / 30.0, 0), 1)
    }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1.0)) { context in
            let now = context.date
            ZStack {
                WatchThemeTokens.backgroundColor().ignoresSafeArea()
                Group {
                    if connectivityManager.accounts.isEmpty {
                        VStack(spacing: 8) {
                            Text("No Accounts")
                                .font(.headline)
                            Text("Open iPhone app and sync to Apple Watch.")
                                .font(.footnote)
                                .multilineTextAlignment(.center)
                                .foregroundStyle(.secondary)
                            if let lastSyncError = connectivityManager.lastSyncError {
                                Text(lastSyncError)
                                    .font(.caption2)
                                    .multilineTextAlignment(.center)
                                    .foregroundStyle(WatchThemeTokens.errorColor())
                            }
                            if let lastSyncTime = connectivityManager.lastSyncTime {
                                Text("Last sync: \(lastSyncTime.formatted(date: .omitted, time: .shortened))")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            Button("Refresh") {
                                connectivityManager.refreshFromLatestAvailableData()
                            }
                            .font(.caption2)
                            .buttonStyle(.bordered)
                        }
                        .padding()
                        #if DEBUG
                        if !connectivityManager.debugEvents.isEmpty {
                            ScrollView {
                                VStack(alignment: .leading, spacing: 2) {
                                    ForEach(Array(connectivityManager.debugEvents.suffix(8).enumerated()), id: \.offset) { _, line in
                                        Text(line)
                                            .font(.system(size: 9, weight: .regular, design: .monospaced))
                                            .foregroundStyle(.secondary)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .multilineTextAlignment(.leading)
                                    }
                                }
                            }
                            .padding(.horizontal, 6)
                        }
                        #endif
                    } else {
                        TabView {
                            ForEach(connectivityManager.accounts, id: \.accountId) { account in
                                VStack(spacing: 10) {
                                    HStack(spacing: 4) {
                                        IssuerBrandIconView(
                                            iconKey: account.issuerIconKey,
                                            size: 18
                                        )
                                        Text(account.issuer ?? "TwoFac")
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                            .lineLimit(1)
                                    }

                                    Text(account.accountLabel)
                                        .font(.footnote)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.75)

                                    Text(connectivityManager.otpCode(for: account))
                                        .font(.system(size: 32, weight: .semibold, design: .monospaced))
                                        .minimumScaleFactor(0.6)
                                        .lineLimit(1)

                                    Text("Expires in \(remainingSeconds(for: now))s")
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)

                                    CountdownBar(progress: elapsedProgress(for: now))
                                        .frame(height: 8)
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 12)
                            }
                        }
                        .tabViewStyle(.verticalPage)
                    }
                }
            }
        }
        .onAppear {
            connectivityManager.refreshFromLatestAvailableData()
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                connectivityManager.refreshFromLatestAvailableData()
            }
        }
    }
}

private struct CountdownBar: View {
    let progress: Double

    private var barColor: Color {
        WatchThemeTokens.timerColor(for: remainingProgress)
    }

    private var remainingProgress: Double {
        1.0 - clampedProgress
    }

    private var clampedProgress: Double {
        min(max(progress, 0.0), 1.0)
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(WatchThemeTokens.timerTrackColor())
                Capsule()
                    .fill(barColor)
                    .frame(width: proxy.size.width * clampedProgress)
            }
        }
    }
}
