# ThreadSafeChatServer

[![Build Status](https://travis-ci.org/RohitMazumder/ThreadSafeChatServer.svg?branch=master)](https://travis-ci.org/RohitMazumder/ThreadSafeChatServer)

## TODO

- [X] Multiple users can send private messages to each other.
- [X] Users are able to login and logoff at any time.
- [X] Prevent users from login using same username.
- [X] Prevent message delivery, if user logs off, or client gets disconnect while their message is in transit.
- [X] If server is full, user login requests are queued till vacancy opens up, in which case they are scheduled using FCFS.
- [ ] Add timestamp to Messages.
- [ ] Add group chat features.
- [ ] Make every user maintain a log of their chats.
- [ ] Handle failed message delivery.
- [ ] Allow sending messages even if receiver is not logged in, in which case, messages will be delivered after receiver logs in.
- [ ] Add unit tests.
- [ ] Document the project.