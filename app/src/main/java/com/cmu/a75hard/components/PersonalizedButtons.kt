package com.cmu.a75hard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmu.a75hard.R
import com.cmu.a75hard.ui.theme.settingsButtonColor

@Composable
fun SettingItem(iconId: Int, label: String, isLogout: Boolean = false, onClick: () -> Unit = {}) {
    val contentColor = if (isLogout) Color.Red else Color.Black
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // Ajuste de espaçamento entre itens
            .background(settingsButtonColor, shape = MaterialTheme.shapes.extraSmall) // Fundo mais claro e forma levemente arredondada
            .clickable { onClick() } // Torna toda a área do item clicável
            .padding(vertical = 17.dp, horizontal = 16.dp) // Ajuste de padding interno
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = "$label Icon",
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, color = contentColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}


@Composable
fun SettingItemWithDropdown(
    iconId: Int,
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(settingsButtonColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(vertical = 17.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = "$label Icon",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { expanded = !expanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedOption,
                        color = Color.Blue,
                        fontSize = 14.sp
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                        contentDescription = "Dropdown Arrow",
                        tint = Color.Blue,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    offset = androidx.compose.ui.unit.DpOffset(x = (-50).dp, y = 10.dp)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            onClick = {
                                expanded = false
                                onOptionSelected(option)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItemWithDropdown_(
    iconId: Int,
    label: String,
    options: List<String>,
    selectedOption: String, // Agora recebemos o valor atual da opção selecionada
    onOptionSelected: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(settingsButtonColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(vertical = 17.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = "$label Icon",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { expanded = !expanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedOption, // Usa o valor fornecido pelo pai
                        color = Color.Blue,
                        fontSize = 14.sp
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                        contentDescription = "Dropdown Arrow",
                        tint = Color.Blue,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    offset = androidx.compose.ui.unit.DpOffset(x = (-50).dp, y = 10.dp)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            onClick = {
                                expanded = false
                                onOptionSelected(option) // Notifica o pai para atualizar o estado
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItemWithToggle(iconId: Int, label: String, isChecked: Boolean, onToggleChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // Espaçamento entre os itens
            .background(settingsButtonColor, shape = MaterialTheme.shapes.extraSmall)
            .height(64.dp) // Altura fixa para garantir consistência com outros itens
            .padding(horizontal = 16.dp) // Padding lateral
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, // Centraliza verticalmente todo o conteúdo
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize() // Preenche todo o espaço da Box
        ) {
            // Ícone e label à esquerda
            Row(
                verticalAlignment = Alignment.CenterVertically // Garante que ícone e texto fiquem alinhados
            ) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = "$label Icon",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Toggle switch à direita
            Switch(
                checked = isChecked,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, // Cor do botão quando ativado
                    checkedTrackColor = Color.Black, // Cor da trilha quando ativado
                    uncheckedThumbColor = Color.Black, // Cor do botão quando desativado
                    uncheckedTrackColor = Color.White, // Cor da trilha quando desativado
                )
            )
        }
    }
}

