package tech.arnav.twofac.cli.screens

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.DrawStyle
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.size
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
            .drawBehind { drawRect(char = '⋅', drawStyle = DrawStyle.Stroke(1)) }
            .padding(2)
            .size(50, 20)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Welcome to 2FAC CLI", modifier = Modifier.fillMaxWidth())
        }
        Row {}
    }

}
