import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Test {

    private static Map<Long, User> users = new HashMap<>();
    private static final Gson gson = new Gson();
    private static final int PORT = 8080;
    public static Long currentId = 1L;

    public static void main(String[] args) throws IOException {
        Map<Long, Post> post1 = new HashMap<>();
        post1.put(1L, new Post(1L, "1 пост "));
        post1.put(2L, new Post(2L, "2 пост"));
        User user1 = new User(currentId, "A", post1);

        Map<Long, Post> post2 = new HashMap<>();
        post2.put(1L, new Post(1L, "1 пост"));
        post2.put(2L, new Post(2L, "2 пост"));
        User user2 = new User(currentId++, "B", post2);

        users.put(user1.id(), user1);
        users.put(user2.id(), user2);

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        httpServer.createContext("/users", Test::handler);
        httpServer.createContext("/users/", Test::handlePostForUsersID);
        httpServer.start();
    }

    public static void handler(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        switch (method) {
            case "GET":
                handleGetForUsers(exchange);
                break;
            case "POST": {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/users")) {
                    handlePostForUsers(exchange);
                } else if (path.startsWith("/users/")) {
                    handlePostForUsersID(exchange);
                } else {
                    sendResponseHeaders(exchange, 404, "Ошибка");
                }
            }
            default:
                sendResponseHeaders(exchange, 404, "Ошибка");
        }
    }

    public static void handleGetForUsers(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/users")) {
            String response = gson.toJson(users.values());
            sendResponseHeaders(exchange, 200, response);
        } else if (path.startsWith("/users/")) {
            long userId = Long.parseLong(path.split("/")[2]);
            User user = getUserById(userId);
            if (user != null) {
                String response = gson.toJson(user);
                sendResponseHeaders(exchange, 200, response);
            } else {
                sendResponseHeaders(exchange, 404, "Ошибка");
            }
        } else {
            sendResponseHeaders(exchange, 404, "Ошибка");
        }
    }

    public static void handlePostForUsers(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(201, 0);
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String body = new String(bytes);
        User newUser = gson.fromJson(body, User.class);
        newUser = new User(currentId++, newUser.name(), newUser.posts());
        users.put(newUser.id(), newUser);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write("Пользователь создан".getBytes());
        }
    }

    public static void handlePostForUsersID(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        long userId = Long.parseLong(path.substring(path.lastIndexOf("/") + 1));
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String body = new String(bytes);
        Post newPost = gson.fromJson(body, Post.class);
        User user = users.get(userId);
        if (user != null) {
            user.posts().put(newPost.id(), newPost);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("Пост обновлен ".getBytes());
            }
        } else {
            sendResponseHeaders(exchange, 404, "Пользователь не найден");
        }

    }

    public static User getUserById(Long id) {
        return users.get(id);
    }


    //    в каждом методе повторялась логика отправки ответа от сервера,поэтому вынесесно в отдельный метод
    public static void sendResponseHeaders(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}

record User(long id, String name, Map<Long, Post> posts) {

}

record Post(long id, String title) {
}