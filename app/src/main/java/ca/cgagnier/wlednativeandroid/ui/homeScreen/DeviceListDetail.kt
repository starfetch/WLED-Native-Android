package ca.cgagnier.wlednativeandroid.ui.homeScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ca.cgagnier.wlednativeandroid.model.Device
import ca.cgagnier.wlednativeandroid.ui.homeScreen.detail.DeviceDetail
import ca.cgagnier.wlednativeandroid.ui.homeScreen.list.DeviceList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DeviceListDetail(
    modifier: Modifier = Modifier,
    viewModel: DeviceListDetailViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val defaultScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val customScaffoldDirective = defaultScaffoldDirective.copy(
        horizontalPartitionSpacerSize = 0.dp,
    )
    val navigator =
        rememberListDetailPaneScaffoldNavigator<Any>(scaffoldDirective = customScaffoldDirective)
    var selectedDevice = navigator.currentDestination?.content as? Device

    val startDiscovery = {
        coroutineScope.launch {
            viewModel.startDiscoveryService()
            delay(15000)
            viewModel.stopDiscoveryService()
        }
    }

    LaunchedEffect(viewModel.isPolling) {
        viewModel.startRefreshDevicesLoop()
    }
    LaunchedEffect("onStart-startDiscovery") {
        startDiscovery()
    }

    Scaffold { innerPadding ->
        NavigableListDetailPaneScaffold(
            modifier = modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            navigator = navigator,
            defaultBackBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
            listPane = {
                AnimatedPane {
                    DeviceList(
                        selectedDevice,
                        onItemClick = { device ->
                            selectedDevice = device
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                content = device
                            )
                        },
                        onRefresh = {
                            viewModel.refreshDevices(silent = false)
                            startDiscovery()
                        },
                        isDiscovering = viewModel.isDiscovering
                    )
                }
            }, detailPane = {
                AnimatedPane {
                    navigator.currentDestination?.content?.let {
                        DeviceDetail(
                            it as Device,
                            canNavigateBack = navigator.canNavigateBack(),
                            navigateUp = {
                                navigator.navigateBack()
                            }
                        )
                    }
                }
            }, extraPane = {
                val content =
                    navigator.currentDestination?.content?.toString() ?: "Select an option"
                AnimatedPane {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = content)
                    }
                }
            }
        )
    }
}