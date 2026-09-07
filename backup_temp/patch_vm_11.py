with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    lines = f.readlines()

replacement = """        messages = _currentChannelId
            .flatMapLatest { channelId ->
                if (channelId.isBlank()) {
                    flowOf(emptyList<Message>())
                } else {
                    repository.getMessagesForChannel(channelId)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        channels = channelRepository.allChannels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeSession = sessionRepository.activeSession.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
"""
new_lines = lines[:326] + [replacement] + lines[352:]

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.writelines(new_lines)
