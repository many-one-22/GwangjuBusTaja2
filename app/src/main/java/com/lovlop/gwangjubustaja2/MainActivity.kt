package com.lovlop.gwangjubustaja2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lovlop.gwangjubustaja2.ui.chat.ChatAdapter
import com.lovlop.gwangjubustaja2.ui.chat.ChatViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var rvChat: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )
            insets
        }

        val etInput = findViewById<EditText>(R.id.etInput)
        val btnSend = findViewById<Button>(R.id.btnSend)
        rvChat = findViewById(R.id.rvChat)

        chatAdapter = ChatAdapter()
        rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
        }

        viewModel.newMessage.observe(this) { message ->
            chatAdapter.addMessage(message)
            if (chatAdapter.itemCount > 0) {
                rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        viewModel.removeLoading.observe(this) { shouldRemove ->
            if (shouldRemove) {
                chatAdapter.removeLastIfLoading()
            }
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                etInput.setText("")
            }
        }
    }
}