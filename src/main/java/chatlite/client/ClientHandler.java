package chatlite.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author zyf
 * @date 2017/9/22
 */
public class ClientHandler {

    private Selector selector;
    private SocketChannel channel;
    private SimpleDateFormat dateFormat;

    public ClientHandler() {
        this("127.0.0.1", 8080);
    }

    public ClientHandler(String serverIp, int port) {
        try {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SS");
            this.channel = SocketChannel.open();
            channel.configureBlocking(false);
            //获得通道管理器
            this.selector = Selector.open();
            //客户端连接服务器，需要调用channel.finishConnect();才能实际完成连接。
            channel.connect(new InetSocketAddress(serverIp, port));
            //为该通道注册SelectionKey.OP_CONNECT事件
            channel.register(selector, SelectionKey.OP_CONNECT);
            channel = SocketChannel.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() throws IOException {
        System.out.println(dateFormat.format(new Date()) + ";客户端正在启动!");
        while (true) {
            //选择注册过的io操作的事件(第一次为SelectionKey.OP_CONNECT)
            selector.select();
            Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey key = ite.next();
                if (key.isConnectable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    channel.configureBlocking(false);

                    //如果正在连接，则完成连接
                    if (channel.isConnectionPending()) {
                        channel.finishConnect();
                    }
                    //向服务器发送消息
//                    channel.write(ByteBuffer.wrap(new String("..."+channel.getRemoteAddress()+"上线了....").getBytes()));
                    //连接成功后，注册接收服务器消息的事件
                    channel.register(selector, SelectionKey.OP_READ);
                    System.out.println(dateFormat.format(new Date()) + ";客户端启动成功!");
                } else if (key.isReadable()) { //有可读数据事件。
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    long length = 0;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((length = channel.read(buffer)) > 0) {
                        buffer.flip();
                        if (buffer.hasRemaining()) {
                            stringBuilder.append(new String(buffer.array()));
                        }
                    }
                    if (length == -1) {
                        channel.close();
                    }
                    buffer.clear();
                    System.out.println(dateFormat.format(new Date())+";来自服务器的消息:" + stringBuilder.toString());
                    channel.register(selector, SelectionKey.OP_WRITE);
                } else if (key.isWritable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    Scanner scanner=new Scanner(System.in);
                    System.out.println("请输入您需要发送的内容,按回车结束!");
                    String message=scanner.nextLine();
                    channel.write(ByteBuffer.wrap("".getBytes()));
                    buffer.flip();
                    try {
                        while (buffer.hasRemaining()) {
                            channel.write(buffer);
                        }
                        buffer.compact();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    channel.register(selector, SelectionKey.OP_READ);
                }
                //删除已选的key，防止重复处理
                ite.remove();
            }
        }
    }


    public void sendMsg() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 8080));
            if (socketChannel.finishConnect()) {
                TimeUnit.SECONDS.sleep(1);
                String info = "I'm " + socketChannel.getLocalAddress() + "-th information from client";
                buffer.clear();
                buffer.put(info.getBytes());
                buffer.flip();
                while (buffer.hasRemaining()) {
                    socketChannel.write(buffer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (socketChannel != null) {
                    socketChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
