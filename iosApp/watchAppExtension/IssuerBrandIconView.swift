import SwiftUI
import CoreText
import TwoFacKit

private enum WatchBrandIconFont {
    static let postScriptName = "FontAwesome6Brands-Regular"

    static let isAvailable: Bool = {
        let postScriptNames = CTFontManagerCopyAvailablePostScriptNames() as? [String] ?? []
        return postScriptNames.contains(postScriptName)
    }()
}

struct IssuerBrandIconView: View {
    let iconKey: String
    var size: CGFloat = 20
    var tint: Color = .secondary
    var accessibilityLabel: String? = nil

    @ViewBuilder
    var body: some View {
        if let accessibilityLabel {
            iconBody
                .accessibilityLabel(Text(accessibilityLabel))
                .accessibilityHidden(false)
        } else {
            iconBody.accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private var iconBody: some View {
        let isPlaceholderKey = iconKey == IssuerIconCatalog.shared.PLACEHOLDER_ICON_KEY
        if !isPlaceholderKey,
           WatchBrandIconFont.isAvailable,
           let glyph = IssuerIconCatalog.shared.glyphForIconKey(iconKey: iconKey) {
            Text(glyph)
                .font(.custom(WatchBrandIconFont.postScriptName, size: size * 0.8))
                .foregroundStyle(tint)
                .frame(width: size, height: size)
        } else {
            ZStack {
                Circle()
                    .stroke(tint.opacity(0.6), lineWidth: 1.2)
                Text("?")
                    .font(.system(size: size * 0.55, weight: .bold, design: .rounded))
                    .foregroundStyle(tint)
            }
            .frame(width: size, height: size)
        }
    }
}
