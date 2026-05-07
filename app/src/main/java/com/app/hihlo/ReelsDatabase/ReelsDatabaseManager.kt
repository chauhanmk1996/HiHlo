package com.app.hihlo.ReelsDatabase

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// ✅ IMPORT YOUR API MODELS (IMPORTANT)
import com.app.hihlo.model.reel.response.Reel
import com.app.hihlo.model.reel.response.Creator
import com.app.hihlo.model.reel.response.Pagination

// =======================
// ✅ PAYLOAD CACHE MODEL
// =======================
data class Payload(
    val reels: List<Reel>?,
    val pagination: Pagination?
)

// =======================
// ✅ DATABASE HELPER
// =======================
class ReelsDatabaseManager(context: Context) :
    SQLiteOpenHelper(context, "reels_db", null, 1) {

    companion object {
        const val TABLE_REELS = "reels"
        const val TABLE_PAGINATION = "pagination"
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE $TABLE_REELS (
                id INTEGER PRIMARY KEY,
                page INTEGER,
                assetType TEXT,
                assetUrl TEXT,
                caption TEXT,
                commentsCount INTEGER,
                isLiked INTEGER,
                isFollowing INTEGER,
                createdAt TEXT,
                creatorId INTEGER,
                creator_name TEXT,
                creator_profileImage TEXT,
                creator_live_status TEXT,
                creator_username TEXT,
                creator_city TEXT,
                creator_country TEXT,
                likesCount INTEGER,
                status TEXT,
                updatedAt TEXT,
                lastPlaybackPosition INTEGER DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_PAGINATION (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                currentPage INTEGER,
                pageSize INTEGER,
                totalItems INTEGER,
                totalPages INTEGER
            )
        """)

        db.execSQL("CREATE INDEX idx_reels_page ON reels(page)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REELS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PAGINATION")
        onCreate(db)
    }

    // =======================
    // ✅ INSERT / UPDATE (UPSERT)
    // =======================
    fun insertOrUpdateReels(
        reels: List<Reel>?,
        page: Int,
        pagination: Pagination?
    ) {
        val db = writableDatabase

        db.beginTransaction()
        try {
            reels?.forEach { reel ->

                val values = ContentValues().apply {
                    put("id", reel.id)
                    put("page", page)

                    put("assetType", reel.assetType)
                    put("assetUrl", reel.assetUrl)
                    put("caption", reel.caption)
                    put("commentsCount", reel.commentsCount)
                    put("isLiked", reel.isLiked)
                    put("isFollowing", reel.isFollowing)
                    put("createdAt", reel.createdAt)
                    put("creatorId", reel.creatorId)

                    put("creator_name", reel.creator.name)
                    put("creator_profileImage", reel.creator.profileImage)
                    put("creator_live_status", reel.creator.user_live_status)
                    put("creator_username", reel.creator.username)
                    put("creator_city", reel.creator.city)
                    put("creator_country", reel.creator.country)

                    put("likesCount", reel.likesCount)
                    put("status", reel.status)
                    put("updatedAt", reel.updatedAt)

                    // preserve old playback if exists
                    val oldPosition = getPlaybackPositionInternal(db, reel.id)
                    put("lastPlaybackPosition", oldPosition)
                }

                db.insertWithOnConflict(
                    TABLE_REELS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }

            // Pagination update
            pagination?.let {
                db.delete(TABLE_PAGINATION, null, null)

                val values = ContentValues().apply {
                    put("currentPage", it.currentPage)
                    put("pageSize", it.pageSize)
                    put("totalItems", it.totalItems)
                    put("totalPages", it.totalPages)
                }

                db.insert(TABLE_PAGINATION, null, values)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    // =======================
    // ✅ INTERNAL PLAYBACK GET
    // =======================
    private fun getPlaybackPositionInternal(db: SQLiteDatabase, reelId: Int): Long {
        val cursor = db.rawQuery(
            "SELECT lastPlaybackPosition FROM $TABLE_REELS WHERE id = ?",
            arrayOf(reelId.toString())
        )

        var pos = 0L
        if (cursor.moveToFirst()) pos = cursor.getLong(0)
        cursor.close()

        return pos
    }

    // =======================
    // ✅ CURSOR → REEL
    // =======================
    private fun cursorToReel(cursor: Cursor): Reel {

        val creator = Creator(
            name = cursor.getString(cursor.getColumnIndexOrThrow("creator_name")),
            profileImage = cursor.getString(cursor.getColumnIndexOrThrow("creator_profileImage")),
            user_live_status = cursor.getString(cursor.getColumnIndexOrThrow("creator_live_status")),
            isStoryUploaded = cursor.getInt(cursor.getColumnIndexOrThrow("isStoryUploaded")),
            username = cursor.getString(cursor.getColumnIndexOrThrow("creator_username")),
            city = cursor.getString(cursor.getColumnIndexOrThrow("creator_city")),
            country = cursor.getString(cursor.getColumnIndexOrThrow("creator_country"))
        )

        return Reel(
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
            assetType = cursor.getString(cursor.getColumnIndexOrThrow("assetType")),
            assetUrl = cursor.getString(cursor.getColumnIndexOrThrow("assetUrl")),
            caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")),
            commentsCount = cursor.getInt(cursor.getColumnIndexOrThrow("commentsCount")),
            isLiked = cursor.getInt(cursor.getColumnIndexOrThrow("isLiked")),
            isFollowing = cursor.getInt(cursor.getColumnIndexOrThrow("isFollowing")),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
            creatorId = cursor.getInt(cursor.getColumnIndexOrThrow("creatorId")),
            creator = creator,
            likesCount = cursor.getInt(cursor.getColumnIndexOrThrow("likesCount")),
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updatedAt")),
            lastPlaybackPosition = cursor.getLong(cursor.getColumnIndexOrThrow("lastPlaybackPosition"))
        )
    }

    // =======================
    // ✅ GET ALL REELS
    // =======================
    fun getAllReels(): List<Reel> {
        val db = readableDatabase
        val list = mutableListOf<Reel>()

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_REELS ORDER BY page ASC, id ASC",
            null
        )

        while (cursor.moveToNext()) {
            list.add(cursorToReel(cursor))
        }

        cursor.close()
        db.close()
        return list
    }

    // =======================
    // ✅ GET REELS BY PAGE
    // =======================
    fun getReelsByPage(page: Int): List<Reel> {
        val db = readableDatabase
        val list = mutableListOf<Reel>()

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_REELS WHERE page = ?",
            arrayOf(page.toString())
        )

        while (cursor.moveToNext()) {
            list.add(cursorToReel(cursor))
        }

        cursor.close()
        db.close()
        return list
    }

    // =======================
    // ✅ GET PLAYBACK POSITION
    // =======================
    fun getPlaybackPosition(reelId: Int): Long {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT lastPlaybackPosition FROM $TABLE_REELS WHERE id = ?",
            arrayOf(reelId.toString())
        )

        var pos = 0L
        if (cursor.moveToFirst()) pos = cursor.getLong(0)

        cursor.close()
        db.close()
        return pos
    }

    // =======================
    // ✅ UPDATE PLAYBACK
    // =======================
    fun updatePlaybackPosition(reelId: Int, position: Long) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put("lastPlaybackPosition", position)
        }

        db.update(TABLE_REELS, values, "id=?", arrayOf(reelId.toString()))
        db.close()
    }

    // =======================
    // ✅ GET PAGE BY REEL ID
    // =======================
    fun getPageByReelId(reelId: Int): Int {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT page FROM $TABLE_REELS WHERE id = ?",
            arrayOf(reelId.toString())
        )

        var page = 1
        if (cursor.moveToFirst()) page = cursor.getInt(0)

        cursor.close()
        db.close()
        return page
    }

    // =======================
    // ✅ GET PAGINATION
    // =======================
    fun getPagination(): Pagination? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_PAGINATION LIMIT 1",
            null
        )

        var pagination: Pagination? = null

        if (cursor.moveToFirst()) {
            pagination = Pagination(
                currentPage = cursor.getInt(cursor.getColumnIndexOrThrow("currentPage")),
                pageSize = cursor.getInt(cursor.getColumnIndexOrThrow("pageSize")),
                totalItems = cursor.getInt(cursor.getColumnIndexOrThrow("totalItems")),
                totalPages = cursor.getInt(cursor.getColumnIndexOrThrow("totalPages"))
            )
        }

        cursor.close()
        db.close()
        return pagination
    }

    // =======================
    // ✅ GET FULL CACHE
    // =======================
    fun getCachedPayload(): Payload? {
        val reels = getAllReels()
        val pagination = getPagination()

        if (reels.isEmpty() && pagination == null) return null

        return Payload(reels, pagination)
    }

    // =======================
    // ✅ GET PAGE CACHE
    // =======================
    fun getPayloadByPage(page: Int): Payload? {
        val reels = getReelsByPage(page)
        val pagination = getPagination()

        if (reels.isEmpty()) return null

        return Payload(reels, pagination)
    }

    fun getReelById(reelId: Int): Reel? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_REELS WHERE id = ? LIMIT 1",
            arrayOf(reelId.toString())
        )

        var reel: Reel? = null

        if (cursor.moveToFirst()) {
            reel = cursorToReel(cursor)
        }

        cursor.close()
        db.close()

        return reel
    }

    fun clearAllData() {
        val db = writableDatabase

        db.beginTransaction()
        try {
            db.delete(TABLE_REELS, null, null)
            db.delete(TABLE_PAGINATION, null, null)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }
}