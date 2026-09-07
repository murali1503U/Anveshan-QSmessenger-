with open("app/src/main/java/com/example/meshchat/data/ChatChannelRepository.kt", "r") as f:
    content = f.read()

content = content.replace("fun getChannelsFlow(isDemo: Boolean): Flow<List<ChatChannel>> = chatChannelDao.getChannelsByDemoFlow(isDemo)", "")

with open("app/src/main/java/com/example/meshchat/data/ChatChannelRepository.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("sessionRepository.getChannelsBySession(sessionId)", "channelRepository.allChannels")

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
