# messenger-server
Redis keys:
- `messenger-service:users:permissions` - info about chats user is engaged in:

        {
            "userId1:objectId1:objectType1": "['edit', 'read', 'delete']",
            "userId2:objectId3:objectType2": "['read', 'invite']",
        }