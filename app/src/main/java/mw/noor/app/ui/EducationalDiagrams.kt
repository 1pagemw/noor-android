package mw.noor.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mw.noor.app.ui.theme.NoorColors

@Composable
fun CellStructureDiagram() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Plant cell", fontWeight = FontWeight.SemiBold, color = NoorColors.MossDark)
                Text("Schematic · not to scale", color = NoorColors.MossDark, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .semantics {
                        contentDescription = "Simplified plant cell showing a cell wall, cell membrane, cytoplasm, nucleus, mitochondria, chloroplasts, and a large permanent vacuole."
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.9f)
                        .height(128.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(NoorColors.MossDark)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(NoorColors.Mint)
                        .border(2.dp, NoorColors.Moss, RoundedCornerShape(26.dp)),
                ) {
                    Text(
                        "Cytoplasm",
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        color = NoorColors.MossDark,
                        fontSize = 11.sp,
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp).size(50.dp),
                        color = NoorColors.Moss,
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("N", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.Center).size(width = 112.dp, height = 62.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, NoorColors.Moss),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Vacuole", color = NoorColors.MossDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 76.dp, top = 27.dp).size(width = 48.dp, height = 20.dp),
                        color = NoorColors.Moss,
                        shape = RoundedCornerShape(50),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Chl.", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 13.dp, bottom = 18.dp).size(width = 50.dp, height = 22.dp),
                        color = NoorColors.MossDark,
                        shape = RoundedCornerShape(50),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Mito.", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StructureLabel("Cell wall", Modifier.weight(1f))
                StructureLabel("Cell membrane", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StructureLabel("Cytoplasm", Modifier.weight(1f))
                StructureLabel("Nucleus", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StructureLabel("Chloroplasts", Modifier.weight(1f))
                StructureLabel("Mitochondria + vacuole", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StructureLabel(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = NoorColors.Mint,
        shape = RoundedCornerShape(9.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = NoorColors.MossDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
