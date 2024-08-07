@file:Suppress(
        "ConstantConditionIf",
        "SimplifyBooleanWithConstants",
        "PrivatePropertyName")

package com.demo.pet.petapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListAdapter
import android.widget.TextView
import com.demo.pet.petapp.conversations.UserDefinitions
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.UnscrollableListView
import com.demo.pet.petapp.util.debugLog

class UserDefinitionConversationsEditorActivity : AppCompatActivity() {
    private val IS_DEBUG = Log.IS_DEBUG || false

    private val keywordProtocols = ArrayList<UserDefinitions.KeywordProtocol>()

    private lateinit var protocolListAdapter: ConversationProtocolListViewAdapter

    private lateinit var conversation_protocol_list: UnscrollableListView
    private lateinit var add_protocol: Button
    private lateinit var input_in_keyword: EditText
    private lateinit var input_out_keyword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        if (IS_DEBUG) debugLog("onCreate() : E")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.user_definition_conversations_editor_activity)

        conversation_protocol_list = findViewById<UnscrollableListView>(R.id.conversation_protocol_list)
        add_protocol = findViewById<Button>(R.id.add_protocol)
        input_in_keyword = findViewById<EditText>(R.id.input_in_keyword)
        input_out_keyword = findViewById<EditText>(R.id.input_out_keyword)

        protocolListAdapter = ConversationProtocolListViewAdapter(
                this,
                R.layout.conversation_protocol_list_item,
                keywordProtocols)
        conversation_protocol_list.adapter = protocolListAdapter

        add_protocol.setOnClickListener(OnAddProtocolClickListenerImpl())

        if (IS_DEBUG) debugLog("onCreate() : X")
    }

    private inner class OnAddProtocolClickListenerImpl : View.OnClickListener {
        override fun onClick(view: View?) {

            val inKeyword = input_in_keyword.text.toString()
            val outKeyword = input_out_keyword.text.toString()

            if (IS_DEBUG) debugLog("Add Protocol : IN=$inKeyword, OUT=$outKeyword")

            keywordProtocols.add(UserDefinitions.KeywordProtocol(inKeyword, outKeyword))

            protocolListAdapter.notifyDataSetChanged()

        }
    }

    override fun onResume() {
        if (IS_DEBUG) debugLog("onResume() : E")
        super.onResume()

        // Load protocol.
        loadKeywordProtocols()

        if (IS_DEBUG) debugLog("onResume() : X")
    }

    private fun loadKeywordProtocols() {
        keywordProtocols.clear()

        val protocolSet = PetApplication.getSP().getStringSet(
                Constants.KEY_KEYWORD_PROTOCOLS,
                setOf<String>()) as Set<String>

        protocolSet.forEach { protocol: String ->
            val keywords = protocol.split("=")
            keywordProtocols.add(UserDefinitions.KeywordProtocol(keywords[0], keywords[1]))
        }
    }

    override fun onPause() {
        if (IS_DEBUG) debugLog("onPause() : E")

        saveKeywordProtocols()

        super.onPause()
        if (IS_DEBUG) debugLog("onPause() : X")
    }

    private fun saveKeywordProtocols() {
        val protocolSet = mutableSetOf<String>()

        keywordProtocols.forEach { protocol: UserDefinitions.KeywordProtocol ->
            protocolSet.add("${protocol.inKeyword}=${protocol.outKeyword}")
        }

        PetApplication.getSP().edit().putStringSet(
                Constants.KEY_KEYWORD_PROTOCOLS,
                protocolSet)
                .apply()
    }

    override fun onDestroy() {
        if (IS_DEBUG) debugLog("onDestroy() : E")

        // NOP.

        super.onDestroy()
        if (IS_DEBUG) debugLog("onDestroy() : X")
    }

    class ConversationProtocolListViewAdapter(
            context: Context,
            private val itemLayoutId: Int,
            private val keywordProtocols: MutableList<UserDefinitions.KeywordProtocol>) : BaseAdapter(), ListAdapter {
        private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
            // Initialize.
            val itemView = view ?: inflater.inflate(itemLayoutId, parent, false)

            val protocol = keywordProtocols[position]
            itemView.findViewById<TextView>(R.id.in_keyword).text = protocol.inKeyword
            itemView.findViewById<TextView>(R.id.out_keyword).text = protocol.outKeyword

            itemView.findViewById<Button>(R.id.del_button).setOnClickListener {
                keywordProtocols.removeAt(position)
                notifyDataSetChanged()
            }

            return itemView
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getCount(): Int {
            return keywordProtocols.size
        }
    }
}
