package com.clashangprpr.app.world

import android.graphics.PointF
import android.graphics.RectF
import com.clashangprpr.app.model.EraType

data class EraBlueprint(
    val playerStart: PointF,
    val merchantPos: PointF,
    val firePos: PointF,
    val craftTablePos: PointF,
    val gatePos: PointF,
    val portalPos: PointF,
    val staticObstacles: List<RectF>,
    val rockCoords: List<Pair<PointF, Boolean>>,
    val crateCoords: List<Pair<PointF, Boolean>>,
    val treeCoords: List<Pair<PointF, Boolean>>,
    val chestCoords: List<Pair<PointF, Int>>,
    val trapCoords: List<Pair<PointF, Boolean>>
)

object EraLayout {
    fun getBlueprint(era: EraType): EraBlueprint {
        return when (era) {
            EraType.ERA_0_HUB -> EraBlueprint(
                playerStart = PointF(1030f, 700f),
                merchantPos = PointF(840f, 640f),
                firePos = PointF(960f, 650f),
                craftTablePos = PointF(1220f, 640f),
                gatePos = PointF(-500f, -500f),
                portalPos = PointF(-500f, -500f),
                staticObstacles = listOf(
                    RectF(0f, 0f, 2160f, 72f),
                    RectF(0f, 1368f, 2160f, 1440f),
                    RectF(0f, 0f, 72f, 1440f),
                    RectF(2088f, 0f, 2160f, 1440f)
                ),
                rockCoords = emptyList(),
                crateCoords = emptyList(),
                treeCoords = emptyList(),
                chestCoords = emptyList(),
                trapCoords = emptyList()
            )

            EraType.ERA_2_OPEN_WORLD -> EraBlueprint(
                playerStart = PointF(950f, 950f),
                merchantPos = PointF(1450f, 1180f),
                firePos = PointF(900f, 890f),
                craftTablePos = PointF(1620f, 1180f),
                gatePos = PointF(8400f, 2400f),
                portalPos = PointF(950f, 700f),
                staticObstacles = OpenWorldChunkSystem().getOpenWorldStaticObstacles(),
                rockCoords = listOf(
                    PointF(1100f, 950f) to false,
                    PointF(1950f, 1850f) to true,
                    PointF(2600f, 2400f) to true,
                    PointF(3200f, 1900f) to false,
                    PointF(1800f, 3400f) to true,
                    PointF(2900f, 3900f) to true
                ),
                crateCoords = listOf(
                    PointF(1250f, 1100f) to false,
                    PointF(2300f, 1600f) to true,
                    PointF(2850f, 2900f) to false,
                    PointF(3500f, 3200f) to true
                ),
                treeCoords = listOf(
                    PointF(920f, 1450f) to false,
                    PointF(1150f, 2100f) to true,
                    PointF(2100f, 2700f) to false,
                    PointF(2450f, 3100f) to true,
                    PointF(3100f, 2500f) to false,
                    PointF(1450f, 3800f) to true
                ),
                chestCoords = listOf(
                    PointF(2200f, 950f) to 90,
                    PointF(2950f, 2200f) to 130,
                    PointF(1650f, 4150f) to 150,
                    PointF(3850f, 3600f) to 180,
                    PointF(4450f, 650f) to 250
                ),
                trapCoords = listOf(
                    PointF(3800f, 1150f) to true,
                    PointF(4050f, 1050f) to false,
                    PointF(4350f, 1050f) to true,
                    PointF(4550f, 1150f) to false
                )
            )

            EraType.ERA_1_MEDIEVAL -> EraBlueprint(
                playerStart = PointF(260f, 700f),
                merchantPos = PointF(340f, 380f),
                firePos = PointF(275f, 395f),
                craftTablePos = PointF(520f, 380f),
                gatePos = PointF(1460f, 720f),
                portalPos = PointF(1920f, 720f),
                staticObstacles = listOf(
                    RectF(0f, 0f, 2160f, 110f),       // Top castle wall
                    RectF(0f, 1330f, 2160f, 1440f),   // Bottom castle wall
                    RectF(0f, 0f, 90f, 1440f),        // Left wall
                    RectF(2070f, 0f, 2160f, 1440f)    // Right wall
                ),
                rockCoords = listOf(
                    PointF(850f, 250f) to false,
                    PointF(1120f, 230f) to true,
                    PointF(1580f, 250f) to false,
                    PointF(1780f, 230f) to true
                ),
                crateCoords = listOf(
                    PointF(780f, 480f) to false,
                    PointF(1260f, 380f) to true,
                    PointF(1620f, 480f) to false
                ),
                treeCoords = listOf(
                    PointF(320f, 980f) to false,
                    PointF(620f, 1080f) to true,
                    PointF(1100f, 1080f) to false,
                    PointF(1420f, 1120f) to true
                ),
                chestCoords = listOf(
                    PointF(180f, 220f) to 45,
                    PointF(1900f, 220f) to 65,
                    PointF(1850f, 1180f) to 85
                ),
                trapCoords = emptyList()
            )

            EraType.ERA_2_CAVES -> EraBlueprint(
                playerStart = PointF(240f, 720f),
                merchantPos = PointF(380f, 460f),
                firePos = PointF(315f, 475f),
                craftTablePos = PointF(560f, 460f),
                gatePos = PointF(1560f, 700f),
                portalPos = PointF(2120f, 720f),
                staticObstacles = listOf(
                    RectF(0f, 0f, 2400f, 90f),
                    RectF(0f, 1350f, 2400f, 1440f),
                    RectF(0f, 0f, 80f, 1440f),
                    RectF(2320f, 0f, 2400f, 1440f),
                    RectF(900f, 140f, 1040f, 280f),   // Cave building 1
                    RectF(1500f, 140f, 1640f, 280f)   // Cave building 2
                ),
                rockCoords = listOf(
                    PointF(760f, 260f) to false,
                    PointF(1180f, 240f) to true,
                    PointF(1740f, 260f) to true,
                    PointF(2040f, 280f) to false
                ),
                crateCoords = listOf(
                    PointF(720f, 640f) to true,
                    PointF(1320f, 520f) to false,
                    PointF(1760f, 560f) to true
                ),
                treeCoords = emptyList(),
                chestCoords = listOf(
                    PointF(180f, 220f) to 55,
                    PointF(2120f, 220f) to 75,
                    PointF(2050f, 1180f) to 95
                ),
                trapCoords = listOf(
                    PointF(980f, 580f) to true,
                    PointF(1220f, 620f) to false,
                    PointF(1380f, 860f) to true,
                    PointF(1680f, 780f) to false
                )
            )

            EraType.ERA_3_ZOMBIE -> EraBlueprint(
                playerStart = PointF(260f, 740f),
                merchantPos = PointF(420f, 520f),
                firePos = PointF(355f, 535f),
                craftTablePos = PointF(610f, 520f),
                gatePos = PointF(1600f, 740f),
                portalPos = PointF(2140f, 740f),
                staticObstacles = listOf(
                    RectF(0f, 0f, 2400f, 110f),
                    RectF(0f, 1330f, 2400f, 1440f),
                    RectF(0f, 0f, 80f, 1440f),
                    RectF(2320f, 0f, 2400f, 1440f)
                ),
                rockCoords = listOf(
                    PointF(880f, 250f) to false,
                    PointF(1240f, 240f) to true,
                    PointF(1700f, 250f) to true
                ),
                crateCoords = listOf(
                    PointF(820f, 580f) to false,
                    PointF(1200f, 460f) to true,
                    PointF(1650f, 540f) to false,
                    PointF(1920f, 480f) to true
                ),
                treeCoords = listOf(
                    PointF(340f, 1020f) to false,
                    PointF(840f, 1080f) to true,
                    PointF(1460f, 1120f) to false
                ),
                chestCoords = listOf(
                    PointF(200f, 240f) to 60,
                    PointF(2120f, 240f) to 85,
                    PointF(2080f, 1180f) to 110
                ),
                trapCoords = listOf(
                    PointF(1050f, 640f) to true,
                    PointF(1350f, 820f) to false,
                    PointF(1750f, 860f) to true
                )
            )

            EraType.ERA_4_ISLAND -> EraBlueprint(
                playerStart = PointF(320f, 920f),
                merchantPos = PointF(460f, 660f),
                firePos = PointF(395f, 675f),
                craftTablePos = PointF(650f, 660f),
                gatePos = PointF(1620f, 920f),
                portalPos = PointF(2120f, 920f),
                staticObstacles = listOf(
                    RectF(0f, 0f, 2400f, 120f),
                    RectF(0f, 1780f, 2400f, 1920f),
                    RectF(0f, 0f, 110f, 1920f),
                    RectF(2280f, 0f, 2400f, 1920f)
                ),
                rockCoords = listOf(
                    PointF(920f, 320f) to false,
                    PointF(1260f, 290f) to true,
                    PointF(1680f, 330f) to true,
                    PointF(1960f, 360f) to false
                ),
                crateCoords = listOf(
                    PointF(860f, 740f) to true,
                    PointF(1320f, 620f) to false,
                    PointF(1740f, 700f) to true
                ),
                treeCoords = listOf(
                    PointF(380f, 1250f) to false,
                    PointF(720f, 1420f) to true,
                    PointF(1200f, 1480f) to false,
                    PointF(1580f, 1420f) to true,
                    PointF(1920f, 1340f) to false
                ),
                chestCoords = listOf(
                    PointF(240f, 260f) to 70,
                    PointF(2100f, 260f) to 95,
                    PointF(2040f, 1580f) to 130
                ),
                trapCoords = listOf(
                    PointF(1080f, 880f) to true,
                    PointF(1420f, 1080f) to false,
                    PointF(1760f, 1020f) to true
                )
            )

            EraType.ERA_5_CYBER -> EraBlueprint(
                playerStart = PointF(220f, 720f),
                merchantPos = PointF(340f, 220f),     // On top cyber sidewalk!
                firePos = PointF(275f, 235f),
                craftTablePos = PointF(520f, 220f),  // On top cyber sidewalk!
                gatePos = PointF(1820f, 720f),
                portalPos = PointF(2140f, 720f),
                staticObstacles = listOf(
                    RectF(0f, 0f, 2400f, 128f),       // Top cyber buildings
                    RectF(0f, 1344f, 2400f, 1440f),   // Bottom cyber buildings
                    RectF(0f, 0f, 64f, 1440f),
                    RectF(2272f, 0f, 2400f, 1440f),
                    RectF(896f, 576f, 1152f, 768f),   // Cyber Block A
                    RectF(1472f, 576f, 1728f, 768f)   // Cyber Block B
                ),
                rockCoords = listOf(
                    PointF(760f, 210f) to true,
                    PointF(1260f, 210f) to true,
                    PointF(1780f, 210f) to false
                ),
                crateCoords = listOf(
                    PointF(740f, 420f) to false,
                    PointF(1280f, 440f) to true,
                    PointF(1280f, 1000f) to false
                ),
                treeCoords = emptyList(),
                chestCoords = listOf(
                    PointF(160f, 1200f) to 90,
                    PointF(2100f, 200f) to 120,
                    PointF(2100f, 1200f) to 140
                ),
                trapCoords = listOf(
                    PointF(1020f, 400f) to true,
                    PointF(1020f, 1020f) to false,
                    PointF(1600f, 400f) to true,
                    PointF(1600f, 1020f) to false
                )
            )

            EraType.ERA_6_VOID -> EraBlueprint(
                playerStart = PointF(220f, 720f),
                merchantPos = PointF(380f, 380f),
                firePos = PointF(315f, 395f),
                craftTablePos = PointF(560f, 380f),
                gatePos = PointF(1620f, 720f),
                portalPos = PointF(2120f, 720f),
                staticObstacles = listOf(
                    RectF(0f, 0f, 2400f, 80f),
                    RectF(0f, 1360f, 2400f, 1440f),
                    RectF(0f, 0f, 72f, 1440f),
                    RectF(2328f, 0f, 2400f, 1440f)
                ),
                rockCoords = listOf(
                    PointF(880f, 240f) to true,
                    PointF(1320f, 240f) to true,
                    PointF(1720f, 240f) to true
                ),
                crateCoords = listOf(
                    PointF(860f, 1060f) to true,
                    PointF(1360f, 1060f) to false
                ),
                treeCoords = emptyList(),
                chestCoords = listOf(
                    PointF(160f, 200f) to 120,
                    PointF(2140f, 200f) to 150,
                    PointF(2140f, 1180f) to 180
                ),
                trapCoords = listOf(
                    PointF(1040f, 560f) to true,
                    PointF(1040f, 880f) to false,
                    PointF(1460f, 560f) to true,
                    PointF(1460f, 880f) to false
                )
            )
        }
    }
}
