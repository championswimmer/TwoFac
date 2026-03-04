package tech.arnav.twofac

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Toolkit

object TrayPositionCalculator {
    fun calculatePopupPosition(popupSize: DpSize): WindowPosition {
        val pointerInfo = try {
            MouseInfo.getPointerInfo()
        } catch (e: Exception) {
            null
        }
        val mouseLocationPx = pointerInfo?.location ?: return WindowPosition(0.dp, 0.dp)
        
        val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenDevice = graphicsEnvironment.screenDevices.firstOrNull { device ->
            device.defaultConfiguration.bounds.contains(mouseLocationPx)
        } ?: graphicsEnvironment.defaultScreenDevice

        val config = screenDevice.defaultConfiguration
        val scale = config.defaultTransform.scaleX.toFloat()
        
        // Convert all AWT pixel-based values to DP
        val mouseX = mouseLocationPx.x / scale
        val mouseY = mouseLocationPx.y / scale
        
        val screenBounds = config.bounds
        val screenX = screenBounds.x / scale
        val screenY = screenBounds.y / scale
        val screenWidth = screenBounds.width / scale
        val screenHeight = screenBounds.height / scale
        
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
        val usableX = screenX + (insets.left / scale)
        val usableY = screenY + (insets.top / scale)
        val usableWidth = screenWidth - ((insets.left + insets.right) / scale)
        val usableHeight = screenHeight - ((insets.top + insets.bottom) / scale)

        val popupWidth = popupSize.width.value
        val popupHeight = popupSize.height.value

        var targetX = mouseX - popupWidth / 2
        var targetY: Float
        
        // Spawn below if mouse is in top half of screen, above if in bottom half
        if (mouseY < screenY + screenHeight / 2) {
            targetY = mouseY + 10 
        } else {
            targetY = mouseY - popupHeight - 10
        }

        // Clamp to usable bounds
        if (targetX < usableX) targetX = usableX
        if (targetX + popupWidth > usableX + usableWidth) targetX = usableX + usableWidth - popupWidth
        
        if (targetY < usableY) targetY = usableY
        if (targetY + popupHeight > usableY + usableHeight) targetY = usableY + usableHeight - popupHeight

        return WindowPosition(targetX.dp, targetY.dp)
    }
}
