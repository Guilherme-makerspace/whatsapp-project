package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// Paleta Telegram
val TgBackground = Color(0xFF17212B)
val TgHeader     = Color(0xFF17212B)
val TgSent       = Color(0xFF2B5278)
val TgReceived   = Color(0xFF182533)
val TgInputBg    = Color(0xFF0E1621)
val TgSidebar    = Color(0xFF0E1621)
val TgAccent     = Color(0xFF5288C1)
val TgText       = Color(0xFFE8E8E8)
val TgMuted      = Color(0xFF708899)

val avatarColors = listOf(
    Color(0xFF4A9052), Color(0xFF9C4A8C), Color(0xFF2E7DB0),
    Color(0xFFB07D2E), Color(0xFF7B4EA0), Color(0xFF2E8C7D)
)

fun avatarColor(name: String) = avatarColors[name.length % avatarColors.size]
fun initials(name: String) = name.trim().split(" ")
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .take(2).joinToString("")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatViewModel = viewModel()
            AppRoot(viewModel)
        }
    }
}

@Composable
fun AppRoot(viewModel: ChatViewModel) {
    val selected = viewModel.selectedContact.value

    if (selected == null) {
        ContactListScreen(viewModel)
    } else {
        ChatScreen(viewModel, selected)
    }
}

// ─── Tela de contatos ──────────────────────────────────────────────────────

@Composable
fun ContactListScreen(viewModel: ChatViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, ip ->
                viewModel.addContact(name, ip)
                showAddDialog = false
            }
        )
    }

    if (showNameDialog) {
        ChangeNameDialog(
            current = viewModel.myName.value,
            onDismiss = { showNameDialog = false },
            onConfirm = { viewModel.myName.value = it; showNameDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TgBackground)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TgHeader)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mensagens", color = TgText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    text = "Você: ${viewModel.myName.value}",
                    color = TgAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { showNameDialog = true }
                )
            }
        }

        HorizontalDivider(color = TgInputBg, thickness = 1.dp)

        // Lista de contatos
        if (viewModel.contacts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhum contato ainda.\nToque em + para adicionar.", color = TgMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.contacts) { contact ->
                    ContactItem(contact, viewModel)
                }
            }
        }

        // Botão adicionar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TgInputBg)
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TgAccent,
                contentColor = Color.White,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar contato")
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, viewModel: ChatViewModel) {
    val messages = viewModel.messagesMap[contact.id]
    val lastMsg = messages?.lastOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectContact(contact) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColor(contact.name)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials(contact.name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, color = TgText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                text = lastMsg?.let { "${it.author}: ${it.text}" } ?: contact.ip,
                color = TgMuted,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = TgInputBg,
        thickness = 0.5.dp
    )
}

// ─── Tela de chat ──────────────────────────────────────────────────────────

@Composable
fun ChatScreen(viewModel: ChatViewModel, contact: Contact) {
    var textState by remember { mutableStateOf("") }
    val messages = viewModel.messagesMap[contact.id] ?: emptyList<Message>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TgBackground)
            .systemBarsPadding()
    ) {
        // Header com voltar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TgHeader)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectedContact.value = null }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TgAccent)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarColor(contact.name)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials(contact.name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(contact.name, color = TgText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(contact.ip, color = TgMuted, fontSize = 12.sp)
            }
        }

        // Mensagens
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TgInputBg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text("Mensagem", color = TgMuted, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TgText,
                    unfocusedTextColor = TgText,
                    focusedContainerColor = Color(0xFF1C2733),
                    unfocusedContainerColor = Color(0xFF1C2733),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = TgAccent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        viewModel.sendMessage(textState)
                        textState = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TgAccent)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isMine) TgSent else TgReceived
    val shape = if (message.isMine)
        RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp)
    else
        RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (!message.isMine) {
                    Text(message.author, color = TgAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(message.text, color = TgText, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

// ─── Dialogs ───────────────────────────────────────────────────────────────

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TgInputBg,
        title = { Text("Novo contato", color = TgText, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome", color = TgMuted) },
                    singleLine = true,
                    colors = outlinedColors()
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP (ex: 10.111.9.8)", color = TgMuted) },
                    singleLine = true,
                    colors = outlinedColors()
                )
                // Atalhos rápidos
                Text("Atalhos:", color = TgMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10.0.2.2", "localhost").forEach { preset ->
                        FilterChip(
                            selected = ip == preset,
                            onClick = { ip = preset },
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TgAccent,
                                selectedLabelColor = TgText,
                                containerColor = TgReceived,
                                labelColor = TgMuted
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && ip.isNotBlank()) onAdd(name, ip) },
                enabled = name.isNotBlank() && ip.isNotBlank()
            ) {
                Text("Adicionar", color = TgAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TgMuted) }
        }
    )
}

@Composable
fun ChangeNameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TgInputBg,
        title = { Text("Seu nome", color = TgText, fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome", color = TgMuted) },
                singleLine = true,
                colors = outlinedColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Salvar", color = TgAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TgMuted) }
        }
    )
}

@Composable
fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TgText,
    unfocusedTextColor = TgText,
    focusedBorderColor = TgAccent,
    unfocusedBorderColor = TgMuted
)