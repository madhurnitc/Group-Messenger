### Group Messenger with Total and FIFO Ordering Guarantees (Java, Android)
_________________
_________________

* The app is a Group Messenger that multicasts a message to all app instances and store them in a permanent key-value storage. (Multicasts also includes the one that is sending the message)

* The app includes ordering guarantees which covers total ordering as well as FIFO ordering. (Ordering implies the receiving order based on the Lamport logical clocks)

* The messages were assigned sequence numbers in order to provide total and FIFO ordering guarantees.

* A content provider is implemented using SQLite on Android to store key-value pair.

* App uses Basic-multicast. It does not implement Reliable-multicast.


Complete description of the project can be read from [here](https://docs.google.com/document/d/1nWaDn2joq-pFmePUjv_hMjO_NrvnmqVmIKGbjET2p5Q/edit)