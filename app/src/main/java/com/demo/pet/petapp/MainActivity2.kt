@file:Suppress("ConstantConditionIf")

package com.demo.pet.petapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.demo.pet.petapp.stt.STTType
import kotlinx.android.synthetic.main.activity_main_2.*
import kotlinx.android.synthetic.main.conversation_protocol_list_item.view.*
import kotlinx.android.synthetic.main.conversation_protocol_list_item_input.*

class MainActivity2 : AppCompatActivity() {

    @Suppress("PrivatePropertyName")
    private val IS_DEBUG = Log.IS_DEBUG

    data class KeywordProtocol(val inKeyword: String, val outKeyword: String)

    private val keywordProtocols = ArrayList<KeywordProtocol>()

    private lateinit var protocolListAdapter: ConversationProtocolListViewAdapter

    companion object {
        fun togglePet(isEnabled: Boolean, context: Context) {
            val action: String = if (isEnabled) {
                REQUEST_START_OVERLAY
            } else {
                REQUEST_STOP_OVERLAY
            }
            val service = Intent(action)
            service.setClass(context , OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service)
            } else {
                context.startService(service)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (IS_DEBUG) debugLog("onCreate() : E")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main_2)

        overlay_switch.isChecked = OverlayRootView.isActive()
        overlay_switch.setOnCheckedChangeListener(OnCheckedChangeListenerImpl())

        protocolListAdapter = ConversationProtocolListViewAdapter(
            this,
            R.layout.conversation_protocol_list_item,
            keywordProtocols)

        conversation_protocol_list.adapter = protocolListAdapter

        add_protocol.setOnClickListener(OnAddProtocolClickListenerImpl())

        if (IS_DEBUG) debugLog("onCreate() : X")
    }

    private inner class OnCheckedChangeListenerImpl : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (IS_DEBUG) debugLog("Overlay switch changed to : $isChecked")

            togglePet(isChecked, this@MainActivity2)
        }
    }

    private inner class OnAddProtocolClickListenerImpl() : View.OnClickListener {
        override fun onClick(view: View?) {

            val inKeyword = input_in_keyword.text.toString()
            val outKeyword = input_out_keyword.text.toString()

            if (IS_DEBUG) debugLog("Add Protocol : IN=$inKeyword, OUT=$outKeyword")

            keywordProtocols.add(KeywordProtocol(inKeyword, outKeyword))

            protocolListAdapter.notifyDataSetChanged()

        }
    }

    override fun onResume() {
        if (IS_DEBUG) debugLog("onResume() : E")
        super.onResume()

        // Mandatory permission check.
        if (isFinishing) {
            return
        }
        if (checkMandatoryPermissions()) {
            return
        }

        // Load protocol.
        loadKeywordProtocols()

        // TTS fixed.
        PetApplication.getSP().edit().putString(
                Constants.KEY_TTS_TYPE,
                TTSType.ANDROID.toString())
                .apply()

        // STT fixed.
        PetApplication.getSP().edit().putString(
                Constants.KEY_STT_TYPE,
                STTType.GOOGLE_WEB_API.toString())
                .apply()

        if (IS_DEBUG) debugLog("onResume() : X")
    }

    private fun loadKeywordProtocols() {
        keywordProtocols.clear()

        val protocolSet = PetApplication.getSP().getStringSet(
                Constants.KEY_KEYWORD_PROTOCOLS,
                setOf<String>())

        protocolSet.forEach { protocol: String ->
            val keywords = protocol.split("=")
            keywordProtocols.add(KeywordProtocol(keywords[0], keywords[1]))
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

        keywordProtocols.forEach { protocol: KeywordProtocol ->
            protocolSet.add("${protocol.inKeyword}=${protocol.outKeyword}")
        }

        PetApplication.getSP().edit().putStringSet(
                Constants.KEY_KEYWORD_PROTOCOLS,
                protocolSet)
                .apply()
    }

    override fun onDestroy() {
        if (IS_DEBUG) debugLog("onDestroy() : E")



        super.onDestroy()
        if (IS_DEBUG) debugLog("onDestroy() : X")
    }

    class ConversationProtocolListViewAdapter(
            context: Context,
            private val itemLayoutId: Int,
            private val keywordProtocols: MutableList<KeywordProtocol>) : BaseAdapter() {
        private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
            val itemView: View

            // Initialize.
            if (view == null) {
                itemView = inflater.inflate(itemLayoutId, parent, false)
            } else {
                itemView = view
            }

            val protocol = keywordProtocols[position]
            itemView.in_keyword.text = protocol.inKeyword
            itemView.out_keyword.text = protocol.outKeyword

            itemView.del_button.setOnClickListener { _: View ->
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

    //// RUNTIME PERMISSION SUPPORT.

    @Suppress("PrivatePropertyName")
    private val REQ_CODE_OVERLAY_PERMISSION: Int = 1000

    @SuppressLint("ObsoleteSdkInt")
    private fun isSystemAlertWindowPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    @SuppressLint("InlinedApi", "ObsoleteSdkInt")
    private fun checkMandatoryPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !isSystemAlertWindowPermissionGranted()) {
            // Start permission setting.
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION)
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQ_CODE_OVERLAY_PERMISSION) {
            if (!isSystemAlertWindowPermissionGranted()) {
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

}




