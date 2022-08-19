package com.app.testmotionsnip.bean

data class MotionBean(
    var totalTime: Double = 0.0,
    var curves: List<MotionCurvesBean> = mutableListOf(),
    var Hips: MotionHipBean = MotionHipBean()
)

data class MotionCurvesBean(
    var name: String = "",
    var path: String = "",
    var rotas: List<PointFourBean> = mutableListOf(),
)

data class MotionHipBean(
    var name: String = "",
    var path: String = "",
    var pos: List<PointThreeBean> = mutableListOf(),
)