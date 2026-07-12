package com.example.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.model.TodoItem
import org.json.JSONArray
import org.json.JSONObject

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodoWidgetFactory(this.applicationContext)
    }
}

class TodoWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val todoItems = mutableListOf<TodoItem>()

    override fun onCreate() {
        loadTodos()
    }

    override fun onDataSetChanged() {
        loadTodos()
    }

    override fun onDestroy() {
        todoItems.clear()
    }

    override fun getCount(): Int {
        return todoItems.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= todoItems.size) {
            return RemoteViews(context.packageName, R.layout.widget_todo_item)
        }

        val item = todoItems[position]
        val views = RemoteViews(context.packageName, R.layout.widget_todo_item)
        
        views.setTextViewText(R.id.tv_todo_text, item.title)
        
        if (item.isCompleted) {
            views.setImageViewResource(R.id.img_checkbox, R.drawable.ic_check)
        } else {
            views.setImageViewResource(R.id.img_checkbox, R.drawable.ic_checkbox_outline)
        }

        // Create fill-in Intent so clicking the row launches the main app
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.tv_todo_text, fillInIntent)
        views.setOnClickFillInIntent(R.id.img_checkbox, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun loadTodos() {
        todoItems.clear()
        try {
            val prefs = context.getSharedPreferences("duet_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("cached_todos_json", null)
            if (!jsonStr.isNullOrEmpty()) {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val jsonObj = arr.getJSONObject(i)
                    val item = TodoItem(
                        todoId = jsonObj.optString("todoId", ""),
                        title = jsonObj.optString("title", ""),
                        isCompleted = jsonObj.optBoolean("isCompleted", false),
                        createdBy = jsonObj.optString("createdBy", ""),
                        createdAt = jsonObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    todoItems.add(item)
                }
                // Sort by latest first
                todoItems.sortByDescending { it.createdAt }
                Log.d("TodoWidgetFactory", "Loaded ${todoItems.size} cached todos from prefs")
            }
        } catch (e: Exception) {
            Log.e("TodoWidgetFactory", "Error parsing cached todos: ${e.message}", e)
        }
    }
}
