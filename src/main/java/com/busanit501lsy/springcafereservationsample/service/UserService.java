package com.busanit501lsy.springcafereservationsample.service;

import com.busanit501lsy.springcafereservationsample.entity.MemberRole;
import com.busanit501lsy.springcafereservationsample.entity.User;
import com.busanit501lsy.springcafereservationsample.entity.mongoEntity.ProfileImage;
import com.busanit501lsy.springcafereservationsample.repository.UserRepository;
import com.busanit501lsy.springcafereservationsample.repository.mongoRepository.ProfileImageRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Log4j2
public class UserService {

    @Autowired
    UserRepository userRepository;

    // 프로필 이미지, 몽고디비 연결
    @Autowired
    ProfileImageRepository profileImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 페이징 처리
    public Page<User> getAllUsersWithPage(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(User user) {
        user.addRole(MemberRole.USER);
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setName(userDetails.getName());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // 프로필 이미지 삭제
        if(user.getProfileImageId() != null && !user.getProfileImageId().isEmpty()) {
            deleteProfileImage(user);
        }
        userRepository.delete(user);
    }

    //프로필 이미지 업로드, 레스트 형식
    public void saveProfileImage(Long userId, MultipartFile file) throws IOException {
        // Get the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create and save the profile image
        ProfileImage profileImage = new ProfileImage(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
        ProfileImage savedImage = profileImageRepository.save(profileImage);

        // Link the profile image to the user
        user.setProfileImageId(savedImage.getId());
        userRepository.save(user);
    }

    public ProfileImage getProfileImage(String imageId) {
        return profileImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
    }

    // 프로필 이미지만 삭제
    public void deleteProfileImage(User user) {
        // 현재 사용자가 가진 프로필 이미지 ID 가져오기
        log.info("lsy user : " + user);
        String profileImageId = user.getProfileImageId();
        log.info("lsy profileImageId : " + profileImageId);

        // 프로필 이미지 ID가 null이 아닌 경우에만 삭제 작업 수행
        if (profileImageId != null) {
            // MongoDB에서 프로필 이미지 삭제
            profileImageRepository.deleteById(profileImageId);

            // 사용자의 profileImageId 필드를 null로 설정
            user.setProfileImageId(null);

            // 업데이트된 사용자 정보 저장
            userRepository.save(user);
        }
    }

}
