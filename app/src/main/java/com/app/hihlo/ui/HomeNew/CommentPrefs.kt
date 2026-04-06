import android.content.Context
import android.util.Log
import com.app.hihlo.model.get_reel_comments.response.Comment
import com.app.hihlo.model.get_reel_comments.response.Payload
import com.google.gson.Gson

object CommentPrefs {

    private const val PREF_NAME = "comment_prefs"
    private const val KEY_PAYLOAD = "key_payload"
    private const val KEY_POST_ID = "key_post_id"

    fun savePayload(context: Context, postId: Int, payload: Payload) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putString(KEY_PAYLOAD, Gson().toJson(payload))
            .putInt(KEY_POST_ID, postId)
            .apply()

        Log.e("CACHE", "Saved postId=$postId size=${payload.comments.size}")
    }

    fun getPayload(context: Context, currentPostId: Int): Payload? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val savedPostId = prefs.getInt(KEY_POST_ID, -1)

        if (savedPostId != currentPostId) {
            clear(context)
            return null
        }

        val json = prefs.getString(KEY_PAYLOAD, null)
        val payload = json?.let { Gson().fromJson(it, Payload::class.java) }

        Log.e("CACHE", "Loaded postId=$currentPostId size=${payload?.comments?.size}")

        return payload
    }

    fun get2Payload(context: Context): Payload? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val json = prefs.getString(KEY_PAYLOAD, null)
        val payload = json?.let { Gson().fromJson(it, Payload::class.java) }

        Log.e("CACHE", "Loaded size=${payload?.comments?.size}")

        return payload
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    // 🔥 FINAL MERGE (NO DUPLICATE + ORDER SAFE)
    fun mergeComments(
        oldList: List<Comment>,
        newList: List<Comment>
    ): Pair<List<Comment>, List<Comment>> {

        val updatedList = oldList.toMutableList()
        val appendedList = mutableListOf<Comment>()

        for (newComment in newList) {

            val index = updatedList.indexOfFirst { it.id == newComment.id }

            if (index != -1) {
                // ✅ update existing
                updatedList[index] = newComment
            } else {
                // ✅ only truly new items
                updatedList.add(newComment)
                appendedList.add(newComment)
            }
        }

        return Pair(updatedList, appendedList)
    }
}