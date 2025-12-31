const stompClient = new StompJs.Client({
	brokerURL: 'ws://localhost:8080/websocket'
});

stompClient.onConnect = frame => {
	setConnected(true);
	console.log('Connected:', frame);
	stompClient.subscribe('/topic/messages', message => {
		showMessage(JSON.parse(message.body).content);
	});
};

stompClient.onWebSocketError = error => {
	console.error('Error with websocket', error);
};

stompClient.onStompError = frame => {
	console.error('Broker reported error:', frame.headers['message']);
	console.error('Additional details:', frame.body);
};

//Caching DOM elements
const connectBtn = document.getElementById('connectBtn');
const disconnectBtn = document.getElementById('disconnectBtn');
const sendBtn = document.getElementById('sendBtn');
const conversation = document.getElementById('conversation');
const messagesList = document.getElementById('messagesList');
const messageInput = document.getElementById('messageInput');

function setConnected(connected) {
	connectBtn.disabled = connected;
	disconnectBtn.disabled = !connected;
	sendBtn.disabled = !connected;
	conversation.hidden = !connected;

	//clear messages list on connect
	messagesList.innerHTML = '';
}

function connect() {
	stompClient.activate();
}

function disconnect() {
	stompClient.deactivate();
	setConnected(false);
	console.log('Disconnected');
}

function sendMessage() {
	stompClient.publish({
		destination: '/app/chat',
		body: JSON.stringify({ content: messageInput.value })
	});
	//clear message input on send
	messageInput.value = '';
	messageInput.focus();
}

function showMessage(message) {
	const li = document.createElement('li');
	li.className = 'list-group-item';
	li.textContent = message;
	messagesList.appendChild(li);
	//scroll to end of list
	messagesList.scrollTop = messagesList.scrollHeight;

}

//DOM ready
document.addEventListener('DOMContentLoaded', () => {
	//prevent default submit for all forms
	document.querySelectorAll('form').forEach(form =>
		form.addEventListener('submit', e => e.preventDefault())
	);

	//click handlers
	connectBtn.addEventListener('click', connect);
	disconnectBtn.addEventListener('click', disconnect);
	sendBtn.addEventListener('click', sendMessage);
});
