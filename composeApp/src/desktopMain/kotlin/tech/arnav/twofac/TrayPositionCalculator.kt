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
        val mouseLocation = pointerInfo?.location ?: return WindowPosition(0.dp, 0.dp)
        
        val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenDevice = graphicsEnvironment.screenDevices.firstOrNull { device ->
            device.defaultConfiguration.bounds.contains(mouseLocation)
        } ?: graphicsEnvironment.defaultScreenDevice

        val screenBounds = screenDevice.defaultConfiguration.bounds
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(screenDevice.defaultConfiguration)
        
        val usableX = screenBounds.x + insets.left
        val usableY = screenBounds.y + insets.top
        val usableWidth = screenBounds.width - insets.left - insets.right
        val usableHeight = screenBounds.height - insets.top - insets.bottom

        val popupWidthPx = popupSize.width.value.toInt()
        val popupHeightPx = popupSize.height.value.toInt()

        var targetX = mouseLocation.x - popupWidthPx / 2
        var targetY = mouseLocation.y
        
        // Spawn below if mouse is in top half, above if in bottom half
        if (mouseLocation.y < screenBounds.y + screenBounds.height / 2) {
            targetY = mouseLocation.y + 10 
        } else {
            targetY = mouseLocation.y - popupHeightPx - 10
        }

        // Clamp to usable bounds
        if (targetX < usableX) targetX = usableX
        if (targetX + popupWidthPx > usableX + usableWidth) targetX = usableX + usableWidth - popupWidthPx
        
        if (targetY < usableY) targetY = usableY
        if (targetY + popupHeightPx > usableY + usableHeight) targetY = usableY + usableHeight - popupHeightPx

        return WindowPosition(targetX.dp, targetY.dp)
    }
}
