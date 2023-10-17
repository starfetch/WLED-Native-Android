package ca.cgagnier.wlednativeandroid.fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import ca.cgagnier.wlednativeandroid.DevicesApplication
import ca.cgagnier.wlednativeandroid.R
import ca.cgagnier.wlednativeandroid.databinding.FragmentDeviceUpdateInstallingBinding
import ca.cgagnier.wlednativeandroid.model.Device
import ca.cgagnier.wlednativeandroid.model.VersionWithAssets
import ca.cgagnier.wlednativeandroid.service.DeviceApiService
import ca.cgagnier.wlednativeandroid.service.api.DownloadState
import ca.cgagnier.wlednativeandroid.service.update.DeviceUpdateService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


private const val DEVICE_ADDRESS = "device_address"
private const val VERSION_TAG = "version_tag"


class DeviceUpdateInstallingFragment : DialogFragment() {
    private lateinit var deviceAddress: String
    private lateinit var device: Device
    private lateinit var versionTag: String
    private lateinit var version: VersionWithAssets

    private var _binding: FragmentDeviceUpdateInstallingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceAddress = it.getString(DEVICE_ADDRESS)!!
            versionTag = it.getString(VERSION_TAG)!!
            loadDeviceAndVersion()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDeviceUpdateInstallingBinding.inflate(layoutInflater, null, false)

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        return binding.root
    }

    private fun loadDeviceAndVersion() {
        val deviceRepository =
            (requireActivity().application as DevicesApplication).deviceRepository
        val versionRepository =
            (requireActivity().application as DevicesApplication).versionWithAssetsRepository
        lifecycleScope.launch {
            device = deviceRepository.findDeviceByAddress(deviceAddress)!!
            version = versionRepository.getVersionByTag(versionTag)!!
            updateFields()
            startUpdate()
        }
    }

    private fun updateFields() {
        binding.textUpdating.text = getString(R.string.updating, device.name)
        binding.textVersionTag.text = version.version.tagName
    }

    private fun startUpdate() {
        binding.textStatus.text = getString(R.string.downloading_binary)
        val updateService = DeviceUpdateService(requireContext(), device, version)
        if (!updateService.couldDetermineAsset()) {
            // TODO Handle no asset found
            return
        }
        val asset = updateService.getAsset()
        binding.textVersionTag.text = asset.name
        binding.progressUpdate.isIndeterminate = false

        lifecycleScope.launch(Dispatchers.IO) {
            updateService.downloadBinary().collect { downloadState ->
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Log.d(TAG, "File download Progress=${downloadState.progress}")
                        activity?.runOnUiThread {
                            binding.progressUpdate.isIndeterminate = false
                            binding.progressUpdate.progress = downloadState.progress
                        }
                    }
                    is DownloadState.Failed -> {
                        Log.e(TAG, "File download Fail")
                        activity?.runOnUiThread {
                            displayFailure()
                        }
                    }
                    is DownloadState.Finished -> {
                        Log.d(TAG, "File download Finished")
                        activity?.runOnUiThread {
                            binding.progressUpdate.isIndeterminate = true
                            installUpdate(updateService)
                        }
                    }
                }
            }
        }
    }

    private fun installUpdate(updateService: DeviceUpdateService) {
        binding.textStatus.text = getString(R.string.installing_update)
        dialog?.setCancelable(false)
        binding.buttonCancel.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            updateService.installUpdate().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    activity?.runOnUiThread {
                        if (response.code() in 201..299) {
                            displaySuccess()
                        } else {
                            Log.d(TAG, "OTA Failed, code ${response.code()}")
                            displayFailure()
                            binding.textUpdatingWarning.text =
                                getString(R.string.ota_install_failed_device_locked)
                            binding.textUpdatingWarning.visibility = View.VISIBLE
                        }
                        updateDeviceUpdated()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    activity?.runOnUiThread {
                        Log.d(TAG, "OTA Failed, call failed")
                        displayFailure()
                    }
                }
            })
        }
    }

    private fun displaySuccess() {
        binding.progressUpdate.visibility = View.INVISIBLE
        binding.textUpdatingWarning.visibility = View.GONE
        binding.imageUpdateSuccess.visibility = View.VISIBLE
        binding.textStatus.text = getString(R.string.update_completed)
        binding.buttonCancel.text = getString(R.string.done)
        dialog?.setCancelable(true)
        binding.buttonCancel.isEnabled = true
    }

    private fun displayFailure() {
        binding.progressUpdate.visibility = View.INVISIBLE
        binding.textUpdatingWarning.visibility = View.GONE
        binding.imageUpdateFailed.visibility = View.VISIBLE
        binding.textStatus.text = getString(R.string.update_failed)
        binding.buttonCancel.text = getString(R.string.done)
        dialog?.setCancelable(true)
        binding.buttonCancel.isEnabled = true
    }

    private fun updateDeviceUpdated() {
        device = device.copy(
            version = version.version.tagName.drop(1),
            newUpdateVersionTagAvailable = ""
        )

        lifecycleScope.launch {
            Log.d(TAG, "Saving deviceUpdated")
            val deviceRepository =
                (requireActivity().application as DevicesApplication).deviceRepository
            deviceRepository.update(device)
            DeviceApiService.update(device, false)
        }
    }

    companion object {
        const val TAG = "DeviceUpdateInstallingFragment"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @param deviceAddress Address of device that can be updated
         * @return A new instance of fragment device_update_installing.
         */
        @JvmStatic
        fun newInstance(deviceAddress: String, versionTag: String) =
            DeviceUpdateInstallingFragment().apply {
                arguments = Bundle().apply {
                    putString(DEVICE_ADDRESS, deviceAddress)
                    putString(VERSION_TAG, versionTag)
                }
            }
    }
}