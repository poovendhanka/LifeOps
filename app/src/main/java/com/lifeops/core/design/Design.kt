package com.lifeops.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Black = Color(0xFF050505)
val Silver = Color(0xFFD8D8DC)
val Muted = Color(0xFFA8A8AE)
val Edge = Color(0xFF39393F)
@Composable fun LifeOpsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary=Silver, onPrimary=Black, background=Black, surface=Color(0xFF111114), surfaceVariant=Color(0xFF1A1A1E), onSurface=Color(0xFFF1F1F3), onSurfaceVariant=Muted, outline=Edge, secondary=Silver), content=content)
}
@Composable fun LifeOpsCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier=modifier.fillMaxWidth().then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier), shape=RoundedCornerShape(24.dp), border=BorderStroke(1.dp, Brush.verticalGradient(listOf(Color(0xFF4B4B52),Color(0xFF242429)))), color=Color.Transparent) {
        Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF19191E),Color(0xFF0D0D10)))).padding(20.dp), verticalArrangement=Arrangement.spacedBy(10.dp), content=content)
    }
}
@Composable fun Section(title:String, action:String?=null, onAction:()->Unit={}) {
    Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=4.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, fontSize=19.sp, fontWeight=FontWeight.Medium)
        if(action!=null) TextButton(onClick=onAction){Text(action,color=Muted)}
    }
}
@Composable fun Eyebrow(text:String) { Text(text.uppercase(), color=Muted, fontSize=11.sp, letterSpacing=1.8.sp, fontWeight=FontWeight.Medium) }
@Composable fun LiquidButton(text:String, onClick:()->Unit, modifier:Modifier=Modifier, enabled:Boolean=true) {
    Button(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=52.dp),shape=RoundedCornerShape(26.dp),colors=ButtonDefaults.buttonColors(containerColor=Silver,contentColor=Black)){Text(text,fontWeight=FontWeight.SemiBold)}
}
@Composable fun EmptyState(title:String, body:String, action:String, onAction:()->Unit) { LifeOpsCard { Eyebrow("A little breathing room"); Text(title,fontSize=23.sp); Text(body,color=Muted); OutlinedButton(onClick=onAction){Text(action)} } }
