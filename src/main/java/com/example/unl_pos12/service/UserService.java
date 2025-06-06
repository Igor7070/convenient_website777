package com.example.unl_pos12.service;

import com.example.unl_pos12.model.messenger.Chat;
import com.example.unl_pos12.model.messenger.User;
import com.example.unl_pos12.repo.ChatRepository;
import com.example.unl_pos12.repo.MessageRepository;
import com.example.unl_pos12.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MessageRepository messageRepository;

    public User createUser(User user) {
        String errorMessage = "";
        // Проверка на существование пользователя
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            errorMessage = "User already exists";
            System.out.println(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        // Проверка на валидность пароля
        if (!isValidPassword(user.getPassword())) {
            errorMessage = "Password must be at least 9 characters long, contain letters and numbers";
            System.out.println(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return userRepository.save(user);
    }

    public User loginUser(String username, String password) {
        String errorMessage = "";
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        } else {
            errorMessage = "Invalid username or password";
            System.out.println(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null); // Возвращает пользователя или null, если не найден
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String getUsername(Long userId) {
        try {
            return getUserById(userId).getUsername();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 9 && password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*");
    }

    public List<Chat> getPrivateChatsByUserId(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            return user.getPrivateChats(); // Возвращаем список приватных чатов
        }
        return Collections.emptyList(); // Возвращаем пустой список, если пользователь не найден
    }

    public boolean deleteChat(Long userId, Long chatId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            List<Chat> chats = user.getPrivateChats();
            boolean isRemoved = chats.removeIf(chat -> chat.getId().equals(chatId));
            if (isRemoved) {
                userRepository.save(user); // Сохраняем обновленного пользователя в базе данных
                return true;
            }
        }
        return false;
    }

    public boolean removeChatIfNotExists(Long chatId) {
        // Получаем чат по ID
        Chat chat = chatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            return false; // Чат не найден
        }

        // Извлекаем имена пользователей из названия чата
        String[] usernames = chat.getName().split("_");
        if (usernames.length != 2) {
            throw new IllegalArgumentException("Chat name must consist of two usernames");
        }

        String username1 = usernames[0];
        String username2 = usernames[1];

        // Получаем пользователей по именам
        User user1 = userRepository.findByUsername(username1).orElse(null);
        User user2 = userRepository.findByUsername(username2).orElse(null);

        if (user1 == null || user2 == null) {
            return false; // Один из пользователей не найден
        }

        // Проверяем наличие чата у обоих пользователей
        boolean chatExistsInUser1 = user1.getPrivateChats().stream().anyMatch(c -> c.getId().equals(chatId));
        boolean chatExistsInUser2 = user2.getPrivateChats().stream().anyMatch(c -> c.getId().equals(chatId));

        if (!chatExistsInUser1 && !chatExistsInUser2) {
            // Сначала удаляем все сообщения, связанные с чатом
            messageRepository.deleteByChatId(chatId);
            // Если чат не найден у ни одного пользователя, удаляем его из базы
            chatRepository.deleteById(chatId); // Удаляем чат по ID
            return true; // Чат успешно удален из базы
        }

        return false; // Чат существует у хотя бы одного пользователя
    }

    public boolean chatExistsForUser(Long userId, Long chatId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            return user.getPrivateChats().stream()
                    .anyMatch(chat -> chat.getId().equals(chatId));
        }
        return false; // Пользователь не найден
    }

    public boolean addChatToUser(Long userId, Long chatId) {
        User user = userRepository.findById(userId).orElse(null);
        Chat chat = chatRepository.findById(chatId).orElse(null);

        if (user != null && chat != null) {
            if (!user.getPrivateChats().contains(chat)) { // Проверяем, что чат еще не добавлен
                user.getPrivateChats().add(chat);
                userRepository.save(user); // Сохраняем обновленного пользователя
                return true; // Чат успешно добавлен
            }
        }
        return false; // Пользователь или чат не найден, или чат уже в списке
    }

    @Transactional // Эта аннотация позволяет выполнять операции в транзакции
    public boolean deleteUser(Long id) {
        // Сначала удаляем все сообщения, отправленные пользователем
        messageRepository.deleteBySenderId(id);

        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public void setUserOnline(Long userId, boolean online) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setOnline(online);
        if (online) {
            user.setLastHeartbeat(System.currentTimeMillis()); // Обновляем метку при установке онлайн
        } else {
            user.setLastHeartbeat(null); // Сбрасываем метку при оффлайн
        }
        userRepository.save(user);
    }

    @Transactional
    public void updateHeartbeat(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setLastHeartbeat(System.currentTimeMillis());
        userRepository.save(user);
    }

    public boolean isUserOnline(Long userId) {
        return userRepository.findById(userId)
                .map(User::isOnline)
                .orElse(false);
    }

    public List<User> getOnlineUsers() {
        return userRepository.findByOnline(true);
    }
}
