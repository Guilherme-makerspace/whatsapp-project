const server = require('http').createServer();
const io = require('socket.io')(server);

io.on('connection', socket => {
    console.log('Usuário conectado:', socket.id);

    socket.on('send_message', (data) => {   
        io.emit('receive_message', data);
    });

    socket.on('disconnect', () => console.log('Usuário saiu'));
});

server.listen(3000, () => console.log('Servidor rodando na porta 3000'));