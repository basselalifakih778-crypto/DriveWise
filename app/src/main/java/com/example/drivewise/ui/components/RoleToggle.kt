package com.example.drivewise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drivewise.domain.model.Role

@Composable
fun RoleToggle(
    selected: Role,
    onRoleSelected: (Role) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Role.entries.forEach { role ->
            FilterChip(
                selected = selected == role,
                onClick = { onRoleSelected(role) },
                label = { Text(role.key.replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

