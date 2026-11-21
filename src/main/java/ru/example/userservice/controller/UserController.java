package ru.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.example.userservice.dto.UserRequestDto;
import ru.example.userservice.dto.UserResponseDto;
import ru.example.userservice.service.UserService;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи", description = "API для управления пользователями (CRUD + HATEOAS)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Создать нового пользователя", description = "Создает пользователя (только с 18 лет) и возвращает его с навигационными ссылками")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации, возраст < 18 лет или email уже занят",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
        UserResponseDto created = userService.createUser(dto);
        addHateoasLinks(created);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Получить пользователя по ID", description = "Возвращает данные пользователя и ссылки на связанные действия")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный ID или пользователь не найден",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(
            @Parameter(description = "ID пользователя", example = "1", required = true)
            @PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        addHateoasLinks(user);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Получить список всех пользователей", description = "Возвращает коллекцию пользователей в формате HAL с HATEOAS-ссылками")
    @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен")
    @GetMapping
    public ResponseEntity<CollectionModel<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        users.forEach(this::addHateoasLinks);

        CollectionModel<UserResponseDto> collectionModel = CollectionModel.of(
                users,
                linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }

    @Operation(summary = "Обновить данные пользователя", description = "Полностью обновляет имя, email и возраст пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные пользователя обновлены",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или пользователь не найден",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @Parameter(description = "ID пользователя", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto dto) {
        UserResponseDto updated = userService.updateUser(id, dto);
        addHateoasLinks(updated);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя из базы данных по его ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален"),
            @ApiResponse(responseCode = "400", description = "Пользователь с указанным ID не найден")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", example = "1", required = true)
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private void addHateoasLinks(UserResponseDto dto) {
        if (dto.getId() != null) {
            dto.add(linkTo(methodOn(UserController.class).getUserById(dto.getId())).withSelfRel());
            dto.add(linkTo(methodOn(UserController.class).updateUser(dto.getId(), null)).withRel("update"));
            dto.add(linkTo(methodOn(UserController.class).deleteUser(dto.getId())).withRel("delete"));
        }
        dto.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));
    }
}
