package com.comet.nfcbasedmdm.mdm.auth.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.comet.nfcbasedmdm.R
import com.comet.nfcbasedmdm.callback.ActivityCallback
import com.comet.nfcbasedmdm.databinding.RegisterLayoutBinding
import com.comet.nfcbasedmdm.mdm.auth.type.ResponseType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var callback: ActivityCallback? = null
    private val viewModel: RegisterViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ActivityCallback
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view : RegisterLayoutBinding = RegisterLayoutBinding.inflate(inflater, container, false)
        initHandler(view)
        initObserver(view)
        return view.root
    }

    // livedata 관측
    private fun initObserver(view : RegisterLayoutBinding) {
        viewModel.requestProcessingLiveData.observe(viewLifecycleOwner) { isProcessing ->
            view.submit.visibility = if (isProcessing)
                View.INVISIBLE
            else
                View.VISIBLE
        }

        viewModel.responseLiveData.observe(viewLifecycleOwner) { response ->
            when (response) {
                ResponseType.OK -> {
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_SHORT).show()
                    callback?.switchToMain()
                }
                ResponseType.PUBLIC_KEY_ERROR -> Toast.makeText(requireContext(), R.string.public_key_error, Toast.LENGTH_SHORT).show()
                ResponseType.INTERNAL_ERROR -> Toast.makeText(requireContext(), R.string.response_error, Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(requireContext(), R.string.response_error, Toast.LENGTH_SHORT).show() // java에서 enum이 null일수 있다 에러로 일단 else branch 제작
            }
        }
    }

    private fun initHandler(view : RegisterLayoutBinding) {
        // 다중 클릭 방지 위해서 확장 함수로 ThrottledClickListener 사용할 수 있음 - CDP2 프로젝트에 사용했음
        view.submit.setOnClickListener {
            val ip = view.server.text.toString()
            if (ip.isEmpty()) {
                Toast.makeText(requireContext(), R.string.url_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.register(ip)
        }
    }

}