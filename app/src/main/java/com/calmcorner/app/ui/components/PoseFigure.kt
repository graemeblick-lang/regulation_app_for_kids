package com.calmcorner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class PosePart { Arms, Hands, Legs, Body }
enum class Prop { Floor, Wall, Chair }

/** Joint positions in unit coordinates (0..1, y down). Right side mirrors left if not given. */
data class Pose(
    val head: Offset, val neck: Offset, val hip: Offset,
    val shL: Offset, val elL: Offset, val haL: Offset,
    val kneeL: Offset, val footL: Offset,
    val shR: Offset? = null, val elR: Offset? = null, val haR: Offset? = null,
    val kneeR: Offset? = null, val footR: Offset? = null,
) {
    /** Front-view helper: right side is a mirror of the left. */
    fun mirrored(): Pose {
        fun m(o: Offset) = Offset(1f - o.x, o.y)
        return copy(shR = m(shL), elR = m(elL), haR = m(haL), kneeR = m(kneeL), footR = m(footL))
    }

    fun lerp(to: Pose, t: Float): Pose {
        fun l(a: Offset, b: Offset) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        fun ln(a: Offset?, b: Offset?, fa: Offset, fb: Offset) = l(a ?: fa, b ?: fb)
        return Pose(
            l(head, to.head), l(neck, to.neck), l(hip, to.hip),
            l(shL, to.shL), l(elL, to.elL), l(haL, to.haL), l(kneeL, to.kneeL), l(footL, to.footL),
            ln(shR, to.shR, shL, to.shL), ln(elR, to.elR, elL, to.elL), ln(haR, to.haR, haL, to.haL),
            ln(kneeR, to.kneeR, kneeL, to.kneeL), ln(footR, to.footR, footL, to.footL),
        )
    }
}

/**
 * Simple friendly person drawn from a [Pose] (checked in a browser mock-up).
 * Active parts are drawn in [deep]; when [tension] > 0 small lines appear around the hands.
 */
@Composable
fun PoseFigure(
    pose: Pose,
    active: Set<PosePart>,
    props: Set<Prop>,
    body: Color,
    limb: Color,
    deep: Color,
    prop: Color,
    tension: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val s = min(size.width, size.height)
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        fun p(o: Offset) = Offset(ox + o.x * s, oy + o.y * s)
        fun col(part: PosePart, base: Color) = if (part in active) deep else base

        if (Prop.Floor in props) drawLine(Color(0x22000000), p(Offset(0.05f, 0.93f)), p(Offset(0.95f, 0.93f)), strokeWidth = 3f)
        if (Prop.Wall in props) drawRect(prop, topLeft = p(Offset(0.82f, 0.02f)), size = androidx.compose.ui.geometry.Size(0.12f * s, 0.9f * s))
        if (Prop.Chair in props) {
            val st = Stroke(0.05f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(Path().apply {
                p(Offset(0.33f, 0.30f)).let { moveTo(it.x, it.y) }
                p(Offset(0.33f, 0.64f)).let { lineTo(it.x, it.y) }
                p(Offset(0.66f, 0.64f)).let { lineTo(it.x, it.y) }
            }, prop, style = st)
            drawLine(prop, p(Offset(0.35f, 0.64f)), p(Offset(0.35f, 0.92f)), strokeWidth = 0.05f * s, cap = StrokeCap.Round)
            drawLine(prop, p(Offset(0.64f, 0.64f)), p(Offset(0.64f, 0.92f)), strokeWidth = 0.05f * s, cap = StrokeCap.Round)
        }

        fun limbPath(pts: List<Offset>, color: Color, w: Float) {
            val path = Path()
            pts.forEachIndexed { i, o -> val q = p(o); if (i == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y) }
            drawPath(path, color, style = Stroke(w * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val legC = col(PosePart.Legs, limb)
        limbPath(listOf(pose.hip, pose.kneeL, pose.footL), color = legC, w = 0.085f)
        limbPath(listOf(pose.hip, pose.kneeR ?: pose.kneeL, pose.footR ?: pose.footL), color = legC, w = 0.085f)
        limbPath(listOf(pose.neck, pose.hip), color = col(PosePart.Body, body), w = 0.17f)
        val armC = col(PosePart.Arms, limb)
        limbPath(listOf(pose.shL, pose.elL, pose.haL), color = armC, w = 0.07f)
        limbPath(listOf(pose.shR ?: pose.shL, pose.elR ?: pose.elL, pose.haR ?: pose.haL), color = armC, w = 0.07f)
        val handC = col(PosePart.Hands, limb)
        val hands = listOf(pose.haL, pose.haR ?: pose.haL)
        hands.forEach { drawCircle(handC, 0.045f * s, p(it)) }
        drawCircle(body, 0.09f * s, p(pose.head))

        if (tension > 0.05f) {
            hands.distinct().forEach { tensionLines(p(it), 0.075f * s, s, deep.copy(alpha = tension)) }
        }
    }
}

private fun DrawScope.tensionLines(c: Offset, rad: Float, s: Float, color: Color) {
    for (i in 0 until 8) {
        val a = i * PI.toFloat() / 4f + 0.4f
        val d = Offset(cos(a), sin(a))
        drawLine(color, c + d * rad, c + d * (rad + 0.03f * s), strokeWidth = 0.012f * s, cap = StrokeCap.Round)
    }
}
