import SwiftUI

struct IssuerBrandIconView: View {
    let iconKey: String
    var size: CGFloat = 20
    var tint: Color = .secondary
    var accessibilityLabel: String? = nil

    @ViewBuilder
    var body: some View {
        let rendered = Group {
            if iconKey == "placeholder" {
                ZStack {
                    Circle()
                        .stroke(tint.opacity(0.6), lineWidth: 1.2)
                    Text("?")
                        .font(.system(size: size * 0.55, weight: .bold, design: .rounded))
                        .foregroundStyle(tint)
                }
            } else {
                Image(iconKey)
                    .resizable()
                    .scaledToFit()
                    .renderingMode(.template)
                    .foregroundStyle(tint)
            }
        }
        .frame(width: size, height: size)

        if let accessibilityLabel {
            rendered
                .accessibilityLabel(Text(accessibilityLabel))
                .accessibilityHidden(false)
        } else {
            rendered.accessibilityHidden(true)
        }
    }
}
