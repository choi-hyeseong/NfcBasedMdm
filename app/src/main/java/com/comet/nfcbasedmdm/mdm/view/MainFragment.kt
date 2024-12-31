package com.comet.nfcbasedmdm.mdm.view

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.comet.nfcbasedmdm.R
import com.comet.nfcbasedmdm.callback.ActivityCallback
import com.comet.nfcbasedmdm.common.receiver.AbstractBroadcastReceiver
import com.comet.nfcbasedmdm.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {

    companion object {
        const val NDM_CHANGE = "NDM_CHANGE"
        const val NDM_SERVER_CHANGE = "NDM_SERVER_CHANGE"
    }


    private var callback: ActivityCallback? = null

    // 메모리 누수 조심..
    private var fragmentMainBinding: FragmentMainBinding? = null

    // 리시버도 메모리 누수 조심
    private var ndmStatusReceiver: NDMStatusReceiver? = null

    // VM
    private val viewModel: MainFragmentViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ActivityCallback
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onStart() {
        super.onStart()
        // 뷰가 보여질때
        registerReceiver()
        //리시버 할당
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(ndmStatusReceiver) //리시버 제거
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentMainBinding = null //destroy시 null 할당
    }

    private fun registerReceiver() {
        val filter = IntentFilter().also {
            it.addAction(NDM_CHANGE)
            it.addAction(NDM_SERVER_CHANGE)
        }
        val receiver = NDMStatusReceiver().also { ndmStatusReceiver = it }
        requireContext().registerReceiver(receiver, filter)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: FragmentMainBinding = FragmentMainBinding.inflate(inflater, container, false)
            .also {
                this.fragmentMainBinding = it //바인딩 초기화
            }
        initObserver(view)

        return view.root
    }


    private fun initObserver(view: FragmentMainBinding) {
        // 서버 상태 observe
        viewModel.serverStatusLiveData.observe(viewLifecycleOwner) { status ->
            val text = if (status) getString(R.string.ndm_server_connected) else getString(R.string.ndm_server_not_connected)
            val color = if (status) requireContext().getColor(R.color.primaryNotifyColor) else requireContext().getColor(R.color.primaryAlertColor)

            view.serverStat.apply {
                this.text = text
                setTextColor(color)
            }
        }

        // mdm 상태 observe
        viewModel.mdmStatusLiveData.observe(viewLifecycleOwner) { status ->
            val text = if (status) getString(R.string.ndm_executed) else getString(R.string.ndm_not_executed)
            val color = if (status) requireContext().getColor(R.color.primaryNotifyColor) else requireContext().getColor(R.color.primaryAlertColor)
            val image = if (status) R.drawable.lockmain else R.drawable.unlock

            view.ndmText.apply {
                this.text = text
                setTextColor(color)
            }
            view.lockImage.setImageResource(image)

        }
    }


    /**
     * 서버상태, MDM상태 확인 위한 리시버
     */
    private inner class NDMStatusReceiver : AbstractBroadcastReceiver() {

        override fun isSupport(action: String): Boolean {
            return action == NDM_CHANGE || action == NDM_SERVER_CHANGE
        }

        // vm으로 데이터 전달해서 화면 회전시에도 데이터 유지할 수 있게
        override fun handleIntent(context: Context, intent: Intent) {
            val status: Boolean = intent.getBooleanExtra("status", false)
            if (intent.action == NDM_CHANGE) viewModel.updateMdmStatus(status)
            else if (intent.action == NDM_SERVER_CHANGE) viewModel.updateServerStatus(status)
        }
    }
}