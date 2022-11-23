package ca.cgagnier.wlednativeandroid.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import ca.cgagnier.wlednativeandroid.R
import ca.cgagnier.wlednativeandroid.DevicesApplication
import ca.cgagnier.wlednativeandroid.databinding.FragmentSettingsBinding
import ca.cgagnier.wlednativeandroid.repository.ThemeSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch


class SettingsFragment : DialogFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentSettingsBinding.inflate(layoutInflater)
        val devicesApp = (requireActivity().application as DevicesApplication)

        lifecycleScope.launch {
            devicesApp.userPreferencesRepository.themeMode.collect {
                when (it) {
                    ThemeSettings.Light -> binding.radioThemeLight.isChecked = true
                    ThemeSettings.Dark -> binding.radioThemeDark.isChecked = true
                    else -> binding.radioThemeAuto.isChecked = true
                }
            }
        }
        lifecycleScope.launch {
            devicesApp.userPreferencesRepository.autoDiscovery.collect {
                binding.switchAutoDiscovery.isChecked = it
            }
        }
        lifecycleScope.launch {
            devicesApp.userPreferencesRepository.showOfflineDevicesLast.collect {
                binding.switchOfflineLast.isChecked = it
            }
        }

        binding.radioThemeGroup.setOnCheckedChangeListener { _, checkedId ->
            lifecycleScope.launch {
                val mode = when(checkedId){
                    R.id.radio_theme_light -> ThemeSettings.Light
                    R.id.radio_theme_dark-> ThemeSettings.Dark
                    else -> ThemeSettings.Auto
                }
                devicesApp.userPreferencesRepository.updateThemeMode(mode)
            }
        }

        // TODO do autoDiscovery system
        binding.switchAutoDiscovery.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                devicesApp.userPreferencesRepository.updateAutoDiscovery(isChecked)
            }
        }

        // TODO When this is on, add a separation before offline devices (Future update)
        binding.switchOfflineLast.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                devicesApp.userPreferencesRepository.updateShowOfflineDeviceLast(isChecked)
            }
        }

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setMessage(R.string.settings)
            .setPositiveButton(R.string.done, null)
            .setView(binding.root)

        return builder.create()
    }
}