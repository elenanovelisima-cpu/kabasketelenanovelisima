package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class SavedVideoManager(private val context: Context) {

    private val baseDir = File(context.filesDir, "saved_analyses").apply { mkdirs() }
    private val videosDir = File(baseDir, "videos").apply { mkdirs() }
    private val thumbnailsDir = File(baseDir, "thumbnails").apply { mkdirs() }
    private val reportsDir = File(baseDir, "reports").apply { mkdirs() }
    private val timelinesDir = File(baseDir, "timelines").apply { mkdirs() }
    private val indexFile = File(baseDir, "index.json")

    @Synchronized
    fun getAllSaved(): List<SavedVideoAnalysis> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val content = indexFile.readText()
            val array = JSONArray(content)
            val list = mutableListOf<SavedVideoAnalysis>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("type", AnalysisType.SHOOTING.name)
                val type = try { AnalysisType.valueOf(typeStr) } catch (e: Exception) { AnalysisType.SHOOTING }
                list.add(
                    SavedVideoAnalysis(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = type,
                        videoFilePath = obj.getString("videoFilePath"),
                        thumbnailPath = obj.optString("thumbnailPath").takeIf { it.isNotEmpty() },
                        durationMs = obj.optLong("durationMs", 0L),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        totalFrames = obj.optInt("totalFrames", 0),
                        attempts = obj.optInt("attempts", 0),
                        makes = obj.optInt("makes", 0),
                        accuracy = obj.optInt("accuracy", 0),
                        avgReleaseAngle = if (obj.has("avgReleaseAngle")) obj.optInt("avgReleaseAngle") else null,
                        possessions = obj.optInt("possessions", 0),
                        fastBreaks = obj.optInt("fastBreaks", 0),
                        pickAndRolls = obj.optInt("pickAndRolls", 0),
                        summaryText = obj.optString("summaryText", ""),
                        hasTacticalReport = obj.optBoolean("hasTacticalReport", false)
                    )
                )
            }
            list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e("SavedVideoManager", "Error reading saved videos index: ${e.message}", e)
            emptyList()
        }
    }

    @Synchronized
    fun saveAnalysis(
        sourceUri: Uri,
        type: AnalysisType,
        customTitle: String? = null,
        durationMs: Long,
        timeline: List<VideoFrameAnalysis>,
        report: TacticalMatchReport?,
        makes: Int,
        attempts: Int,
        firstFrameBitmap: Bitmap?
    ): SavedVideoAnalysis {
        val id = UUID.randomUUID().toString()
        val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val defaultTitle = if (type == AnalysisType.SHOOTING) {
            "Sesión de Tiros ($dateFormatted)"
        } else {
            "Partido Táctico ($dateFormatted)"
        }
        val title = customTitle?.takeIf { it.isNotBlank() } ?: defaultTitle

        // 1. Copy video file to persistent app storage
        val targetVideoFile = File(videosDir, "$id.mp4")
        try {
            if (sourceUri.scheme == "file") {
                val srcFile = File(sourceUri.path ?: "")
                if (srcFile.exists()) {
                    srcFile.copyTo(targetVideoFile, overwrite = true)
                }
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    targetVideoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SavedVideoManager", "Error copying video file: ${e.message}", e)
        }

        // 2. Save thumbnail
        var thumbnailPath: String? = null
        if (firstFrameBitmap != null && !firstFrameBitmap.isRecycled) {
            try {
                val thumbFile = File(thumbnailsDir, "$id.jpg")
                FileOutputStream(thumbFile).use { out ->
                    firstFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                thumbnailPath = thumbFile.absolutePath
            } catch (e: Exception) {
                Log.e("SavedVideoManager", "Error saving thumbnail: ${e.message}", e)
            }
        }

        // 3. Save report JSON if available
        var hasTacticalReport = false
        if (report != null) {
            try {
                val reportJson = JSONObject().apply {
                    put("matchTitle", report.matchTitle)
                    put("generatedAt", report.generatedAt)
                    put("durationFormatted", report.durationFormatted)
                    put("totalPossessions", report.totalPossessions)
                    put("homePossessionPct", report.homePossessionPct)
                    put("awayPossessionPct", report.awayPossessionPct)
                    put("fastBreakCount", report.fastBreakCount)
                    put("fastBreakSuccessRate", report.fastBreakSuccessRate)
                    put("fastBreakPoints", report.fastBreakPoints)
                    put("avgTransitionTimeSec", report.avgTransitionTimeSec.toDouble())
                    put("fullCourtPressCount", report.fullCourtPressCount)
                    put("fullCourtPressBreakRate", report.fullCourtPressBreakRate)
                    put("forcedTurnoversFromPress", report.forcedTurnoversFromPress)
                    put("pickAndRollCount", report.pickAndRollCount)
                    put("pickAndRollRollCount", report.pickAndRollRollCount)
                    put("pickAndRollPopCount", report.pickAndRollPopCount)
                    put("pickAndRollPointsPerPlay", report.pickAndRollPointsPerPlay.toDouble())
                    put("homeAverageSpacing", report.homeAverageSpacing)
                    put("awayAverageSpacing", report.awayAverageSpacing)
                    put("manToManPossessions", report.manToManPossessions)
                    put("zoneDefensePossessions", report.zoneDefensePossessions)
                    put("dominantOffensiveStyle", report.dominantOffensiveStyle)
                    put("dominantDefensiveStyle", report.dominantDefensiveStyle)

                    val takeawaysArray = JSONArray()
                    report.tacticalTakeaways.forEach { takeawaysArray.put(it) }
                    put("tacticalTakeaways", takeawaysArray)
                }
                File(reportsDir, "$id.json").writeText(reportJson.toString())
                hasTacticalReport = true
            } catch (e: Exception) {
                Log.e("SavedVideoManager", "Error saving report JSON: ${e.message}", e)
            }
        }

        // 3.5. Save timeline for 60 FPS smooth instant playback
        if (timeline.isNotEmpty()) {
            saveTimelineToFile(id, timeline)
        }

        val accuracy = if (attempts > 0) ((makes.toFloat() / attempts.toFloat()) * 100).toInt() else 0
        val summaryText = if (type == AnalysisType.SHOOTING) {
            "$makes de $attempts encestados ($accuracy% de acierto) • ${timeline.size} fotogramas"
        } else {
            "${report?.totalPossessions ?: 0} posesiones • ${report?.fastBreakCount ?: 0} contraataques • ${report?.pickAndRollCount ?: 0} pick & roll"
        }

        val saved = SavedVideoAnalysis(
            id = id,
            title = title,
            type = type,
            videoFilePath = targetVideoFile.absolutePath,
            thumbnailPath = thumbnailPath,
            durationMs = durationMs,
            createdAt = System.currentTimeMillis(),
            totalFrames = timeline.size,
            attempts = attempts,
            makes = makes,
            accuracy = accuracy,
            possessions = report?.totalPossessions ?: 0,
            fastBreaks = report?.fastBreakCount ?: 0,
            pickAndRolls = report?.pickAndRollCount ?: 0,
            summaryText = summaryText,
            hasTacticalReport = hasTacticalReport
        )

        // 4. Update index.json
        val currentList = getAllSaved().toMutableList()
        currentList.add(0, saved)
        writeIndex(currentList)

        return saved
    }

    @Synchronized
    fun deleteSaved(id: String): Boolean {
        try {
            File(videosDir, "$id.mp4").delete()
            File(thumbnailsDir, "$id.jpg").delete()
            File(reportsDir, "$id.json").delete()
            File(timelinesDir, "$id.timeline.gz").delete()
            File(timelinesDir, "$id.timeline.json").delete()

            val currentList = getAllSaved().filter { it.id != id }
            writeIndex(currentList)
            return true
        } catch (e: Exception) {
            Log.e("SavedVideoManager", "Error deleting saved video: ${e.message}", e)
            return false
        }
    }

    fun loadTimeline(id: String): List<VideoFrameAnalysis> {
        val gzFile = File(timelinesDir, "$id.timeline.gz")
        val jsonFile = File(timelinesDir, "$id.timeline.json")
        val fileToRead = when {
            gzFile.exists() -> gzFile
            jsonFile.exists() -> jsonFile
            else -> return emptyList()
        }

        return try {
            val jsonString = if (fileToRead.name.endsWith(".gz")) {
                GZIPInputStream(FileInputStream(fileToRead)).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                fileToRead.readText()
            }
            val array = JSONArray(jsonString)
            val list = ArrayList<VideoFrameAnalysis>(array.length())

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val timestampMs = obj.optLong("t", 0L)

                val ball = if (obj.has("b")) {
                    val bArr = obj.getJSONArray("b")
                    Det(
                        cx = bArr.getDouble(0).toFloat(),
                        cy = bArr.getDouble(1).toFloat(),
                        w = bArr.getDouble(2).toFloat(),
                        h = bArr.getDouble(3).toFloat(),
                        conf = bArr.getDouble(4).toFloat()
                    )
                } else null

                val hoop = if (obj.has("h")) {
                    val hArr = obj.getJSONArray("h")
                    Det(
                        cx = hArr.getDouble(0).toFloat(),
                        cy = hArr.getDouble(1).toFloat(),
                        w = hArr.getDouble(2).toFloat(),
                        h = hArr.getDouble(3).toFloat(),
                        conf = hArr.getDouble(4).toFloat()
                    )
                } else null

                val player = if (obj.has("p")) {
                    val pArr = obj.getJSONArray("p")
                    Det(
                        cx = pArr.getDouble(0).toFloat(),
                        cy = pArr.getDouble(1).toFloat(),
                        w = pArr.getDouble(2).toFloat(),
                        h = pArr.getDouble(3).toFloat(),
                        conf = pArr.getDouble(4).toFloat()
                    )
                } else null

                val skeleton = if (obj.has("s")) {
                    val skObj = obj.getJSONObject("s")
                    val lmArr = skObj.getJSONArray("lm")
                    val lmList = ArrayList<PosePoint>(lmArr.length())
                    for (k in 0 until lmArr.length()) {
                        val ptArr = lmArr.getJSONArray(k)
                        lmList.add(
                            PosePoint(
                                x = ptArr.getDouble(0).toFloat(),
                                y = ptArr.getDouble(1).toFloat(),
                                z = if (ptArr.length() > 2) ptArr.getDouble(2).toFloat() else 0f,
                                visibility = if (ptArr.length() > 3) ptArr.getDouble(3).toFloat() else 1f
                            )
                        )
                    }

                    val wrPoint = if (skObj.has("wr")) {
                        val wrArr = skObj.getJSONArray("wr")
                        PosePoint(wrArr.getDouble(0).toFloat(), wrArr.getDouble(1).toFloat())
                    } else null

                    val ftPoint = if (skObj.has("ft")) {
                        val ftArr = skObj.getJSONArray("ft")
                        PosePoint(ftArr.getDouble(0).toFloat(), ftArr.getDouble(1).toFloat())
                    } else null

                    val releaseAngle = if (skObj.has("ra")) skObj.getInt("ra") else null
                    val isShootingMotion = skObj.optBoolean("sm", false)

                    PoseSkeleton(
                        landmarks = lmList,
                        wristReleasePoint = wrPoint,
                        feetCourtPoint = ftPoint,
                        releaseAngle = releaseAngle,
                        isShootingMotion = isShootingMotion
                    )
                } else null

                val courtShots = if (obj.has("cs")) {
                    val csArr = obj.getJSONArray("cs")
                    val csList = ArrayList<CourtShotPoint>(csArr.length())
                    for (k in 0 until csArr.length()) {
                        val pt = csArr.getJSONArray(k)
                        csList.add(
                            CourtShotPoint(
                                xNorm = pt.getDouble(0).toFloat(),
                                yNorm = pt.getDouble(1).toFloat(),
                                made = pt.getBoolean(2)
                            )
                        )
                    }
                    csList
                } else emptyList()

                val shots = if (obj.has("sh")) {
                    val shArr = obj.getJSONArray("sh")
                    val shList = ArrayList<ShotEntry>(shArr.length())
                    for (k in 0 until shArr.length()) {
                        val shObj = shArr.getJSONObject(k)
                        val loc = try {
                            ShotLocation.valueOf(shObj.optString("loc", ShotLocation.CENTER.name))
                        } catch (_: Exception) { ShotLocation.CENTER }
                        val cp = if (shObj.has("cp")) {
                            val cpArr = shObj.getJSONArray("cp")
                            CourtShotPoint(
                                xNorm = cpArr.getDouble(0).toFloat(),
                                yNorm = cpArr.getDouble(1).toFloat(),
                                made = cpArr.getBoolean(2)
                            )
                        } else null
                        shList.add(
                            ShotEntry(
                                id = shObj.optString("id", UUID.randomUUID().toString()),
                                made = shObj.optBoolean("m", false),
                                shotLocation = loc,
                                takenAt = shObj.optString("t", ""),
                                releaseAngle = if (shObj.has("ra")) shObj.getInt("ra") else null,
                                courtPoint = cp
                            )
                        )
                    }
                    shList
                } else emptyList()

                val tacticalAnalysis = if (obj.has("tac")) {
                    val tacObj = obj.getJSONObject("tac")
                    val plArr = tacObj.optJSONArray("pl")
                    val plList = ArrayList<TacticalPlayerTrack>()
                    if (plArr != null) {
                        for (k in 0 until plArr.length()) {
                            val pObj = plArr.getJSONObject(k)
                            val team = try { TacticalTeam.valueOf(pObj.getString("tm")) } catch (_: Exception) { TacticalTeam.UNKNOWN }
                            plList.add(
                                TacticalPlayerTrack(
                                    id = pObj.getInt("id"),
                                    xNorm = pObj.getDouble("x").toFloat(),
                                    yNorm = pObj.getDouble("y").toFloat(),
                                    team = team,
                                    isWithBall = pObj.optBoolean("wb", false),
                                    speedNorm = pObj.optDouble("sp", 0.0).toFloat(),
                                    role = pObj.optString("rl", "Jugador")
                                )
                            )
                        }
                    }

                    val ballPos = if (tacObj.has("bp")) {
                        val bpArr = tacObj.getJSONArray("bp")
                        Pair(bpArr.getDouble(0).toFloat(), bpArr.getDouble(1).toFloat())
                    } else null

                    val dominantTeam = try {
                        TacticalTeam.valueOf(tacObj.optString("dom", TacticalTeam.HOME.name))
                    } catch (_: Exception) { TacticalTeam.HOME }

                    val activePlayBadge = if (tacObj.has("bdg")) {
                        try { TacticalPlayType.valueOf(tacObj.getString("bdg")) } catch (_: Exception) { null }
                    } else null

                    TacticalFrameAnalysis(
                        timestampMs = tacObj.optLong("t", timestampMs),
                        players = plList,
                        ballPosition = ballPos,
                        offensiveSpacingArea = tacObj.optDouble("spc", 0.5).toFloat(),
                        isPressingFullCourt = tacObj.optBoolean("prs", false),
                        isFastBreak = tacObj.optBoolean("fb", false),
                        isPickAndRollOccurring = tacObj.optBoolean("pnr", false),
                        dominantTeamWithBall = dominantTeam,
                        activePlayBadge = activePlayBadge,
                        activePlayDescription = tacObj.optString("dsc").takeIf { it.isNotEmpty() }
                    )
                } else null

                val loc = if (obj.has("loc")) {
                    try { ShotLocation.valueOf(obj.getString("loc")) } catch (_: Exception) { null }
                } else null

                list.add(
                    VideoFrameAnalysis(
                        timestampMs = timestampMs,
                        ball = ball,
                        hoop = hoop,
                        player = player,
                        skeleton = skeleton,
                        attempts = obj.optInt("att", 0),
                        makes = obj.optInt("mk", 0),
                        misses = obj.optInt("ms", 0),
                        accuracy = obj.optInt("ac", 0),
                        currentStreak = obj.optInt("st", 0),
                        lastEvent = obj.optString("ev", "—"),
                        lastLocation = loc,
                        releaseAngle = if (obj.has("ra")) obj.getInt("ra") else null,
                        releaseTimeSec = if (obj.has("rt")) obj.getDouble("rt").toFloat() else null,
                        shots = shots,
                        courtShots = courtShots,
                        tacticalAnalysis = tacticalAnalysis
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("SavedVideoManager", "Error loading timeline for $id: ${e.message}", e)
            emptyList()
        }
    }

    private fun saveTimelineToFile(id: String, timeline: List<VideoFrameAnalysis>) {
        try {
            val file = File(timelinesDir, "$id.timeline.gz")
            val array = JSONArray()
            for (f in timeline) {
                val obj = JSONObject()
                obj.put("t", f.timestampMs)
                f.ball?.let { b ->
                    val bArr = JSONArray().apply {
                        put(b.cx.toDouble())
                        put(b.cy.toDouble())
                        put(b.w.toDouble())
                        put(b.h.toDouble())
                        put(b.conf.toDouble())
                    }
                    obj.put("b", bArr)
                }
                f.hoop?.let { h ->
                    val hArr = JSONArray().apply {
                        put(h.cx.toDouble())
                        put(h.cy.toDouble())
                        put(h.w.toDouble())
                        put(h.h.toDouble())
                        put(h.conf.toDouble())
                    }
                    obj.put("h", hArr)
                }
                f.player?.let { p ->
                    val pArr = JSONArray().apply {
                        put(p.cx.toDouble())
                        put(p.cy.toDouble())
                        put(p.w.toDouble())
                        put(p.h.toDouble())
                        put(p.conf.toDouble())
                    }
                    obj.put("p", pArr)
                }
                f.skeleton?.let { sk ->
                    val skObj = JSONObject()
                    val lmArr = JSONArray()
                    for (lm in sk.landmarks) {
                        val pt = JSONArray().apply {
                            put(lm.x.toDouble())
                            put(lm.y.toDouble())
                            put(lm.z.toDouble())
                            put(lm.visibility.toDouble())
                        }
                        lmArr.put(pt)
                    }
                    skObj.put("lm", lmArr)
                    sk.wristReleasePoint?.let { wr ->
                        skObj.put("wr", JSONArray().apply { put(wr.x.toDouble()); put(wr.y.toDouble()) })
                    }
                    sk.feetCourtPoint?.let { ft ->
                        skObj.put("ft", JSONArray().apply { put(ft.x.toDouble()); put(ft.y.toDouble()) })
                    }
                    sk.releaseAngle?.let { skObj.put("ra", it) }
                    skObj.put("sm", sk.isShootingMotion)
                    obj.put("s", skObj)
                }
                if (f.attempts != 0) obj.put("att", f.attempts)
                if (f.makes != 0) obj.put("mk", f.makes)
                if (f.misses != 0) obj.put("ms", f.misses)
                if (f.accuracy != 0) obj.put("ac", f.accuracy)
                if (f.currentStreak != 0) obj.put("st", f.currentStreak)
                if (f.lastEvent != "—") obj.put("ev", f.lastEvent)
                f.lastLocation?.let { obj.put("loc", it.name) }
                f.releaseAngle?.let { obj.put("ra", it) }
                f.releaseTimeSec?.let { obj.put("rt", it.toDouble()) }

                if (f.courtShots.isNotEmpty()) {
                    val csArr = JSONArray()
                    for (cs in f.courtShots) {
                        csArr.put(JSONArray().apply {
                            put(cs.xNorm.toDouble())
                            put(cs.yNorm.toDouble())
                            put(cs.made)
                        })
                    }
                    obj.put("cs", csArr)
                }

                if (f.shots.isNotEmpty()) {
                    val shArr = JSONArray()
                    for (sh in f.shots) {
                        val shObj = JSONObject().apply {
                            put("id", sh.id)
                            put("m", sh.made)
                            put("loc", sh.shotLocation.name)
                            put("t", sh.takenAt)
                            sh.releaseAngle?.let { put("ra", it) }
                            sh.courtPoint?.let { cp ->
                                put("cp", JSONArray().apply {
                                    put(cp.xNorm.toDouble())
                                    put(cp.yNorm.toDouble())
                                    put(cp.made)
                                })
                            }
                        }
                        shArr.put(shObj)
                    }
                    obj.put("sh", shArr)
                }

                f.tacticalAnalysis?.let { tac ->
                    val tacObj = JSONObject()
                    tacObj.put("t", tac.timestampMs)
                    val pList = JSONArray()
                    for (tp in tac.players) {
                        val pObj = JSONObject().apply {
                            put("id", tp.id)
                            put("x", tp.xNorm.toDouble())
                            put("y", tp.yNorm.toDouble())
                            put("tm", tp.team.name)
                            put("wb", tp.isWithBall)
                            put("sp", tp.speedNorm.toDouble())
                            put("rl", tp.role)
                        }
                        pList.put(pObj)
                    }
                    tacObj.put("pl", pList)
                    tac.ballPosition?.let { bp ->
                        tacObj.put("bp", JSONArray().apply { put(bp.first.toDouble()); put(bp.second.toDouble()) })
                    }
                    tacObj.put("spc", tac.offensiveSpacingArea.toDouble())
                    tacObj.put("prs", tac.isPressingFullCourt)
                    tacObj.put("fb", tac.isFastBreak)
                    tacObj.put("pnr", tac.isPickAndRollOccurring)
                    tacObj.put("dom", tac.dominantTeamWithBall.name)
                    tac.activePlayBadge?.let { tacObj.put("bdg", it.name) }
                    tac.activePlayDescription?.let { tacObj.put("dsc", it) }
                    obj.put("tac", tacObj)
                }

                array.put(obj)
            }

            GZIPOutputStream(FileOutputStream(file)).use { gzip ->
                gzip.write(array.toString().toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.e("SavedVideoManager", "Error saving timeline for $id: ${e.message}", e)
        }
    }

    fun loadTacticalReport(id: String): TacticalMatchReport? {
        val file = File(reportsDir, "$id.json")
        if (!file.exists()) return null
        return try {
            val obj = JSONObject(file.readText())
            val takeaways = mutableListOf<String>()
            val takeawaysArray = obj.optJSONArray("tacticalTakeaways")
            if (takeawaysArray != null) {
                for (i in 0 until takeawaysArray.length()) {
                    takeaways.add(takeawaysArray.getString(i))
                }
            }

            TacticalMatchReport(
                matchTitle = obj.optString("matchTitle", "Informe Táctico"),
                generatedAt = obj.optString("generatedAt", ""),
                durationFormatted = obj.optString("durationFormatted", "00:00"),
                totalPossessions = obj.optInt("totalPossessions", 0),
                homePossessionPct = obj.optInt("homePossessionPct", 50),
                awayPossessionPct = obj.optInt("awayPossessionPct", 50),
                fastBreakCount = obj.optInt("fastBreakCount", 0),
                fastBreakSuccessRate = obj.optInt("fastBreakSuccessRate", 0),
                fastBreakPoints = obj.optInt("fastBreakPoints", 0),
                avgTransitionTimeSec = obj.optDouble("avgTransitionTimeSec", 3.0).toFloat(),
                fullCourtPressCount = obj.optInt("fullCourtPressCount", 0),
                fullCourtPressBreakRate = obj.optInt("fullCourtPressBreakRate", 0),
                forcedTurnoversFromPress = obj.optInt("forcedTurnoversFromPress", 0),
                pickAndRollCount = obj.optInt("pickAndRollCount", 0),
                pickAndRollRollCount = obj.optInt("pickAndRollRollCount", 0),
                pickAndRollPopCount = obj.optInt("pickAndRollPopCount", 0),
                pickAndRollPointsPerPlay = obj.optDouble("pickAndRollPointsPerPlay", 1.0).toFloat(),
                homeAverageSpacing = obj.optInt("homeAverageSpacing", 70),
                awayAverageSpacing = obj.optInt("awayAverageSpacing", 70),
                manToManPossessions = obj.optInt("manToManPossessions", 0),
                zoneDefensePossessions = obj.optInt("zoneDefensePossessions", 0),
                dominantOffensiveStyle = obj.optString("dominantOffensiveStyle", ""),
                dominantDefensiveStyle = obj.optString("dominantDefensiveStyle", ""),
                tacticalTakeaways = takeaways
            )
        } catch (e: Exception) {
            Log.e("SavedVideoManager", "Error parsing saved report: ${e.message}", e)
            null
        }
    }

    private fun writeIndex(list: List<SavedVideoAnalysis>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("type", item.type.name)
                    put("videoFilePath", item.videoFilePath)
                    put("thumbnailPath", item.thumbnailPath ?: "")
                    put("durationMs", item.durationMs)
                    put("createdAt", item.createdAt)
                    put("totalFrames", item.totalFrames)
                    put("attempts", item.attempts)
                    put("makes", item.makes)
                    put("accuracy", item.accuracy)
                    item.avgReleaseAngle?.let { put("avgReleaseAngle", it) }
                    put("possessions", item.possessions)
                    put("fastBreaks", item.fastBreaks)
                    put("pickAndRolls", item.pickAndRolls)
                    put("summaryText", item.summaryText)
                    put("hasTacticalReport", item.hasTacticalReport)
                }
                array.put(obj)
            }
            indexFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.e("SavedVideoManager", "Error writing index: ${e.message}", e)
        }
    }
}
