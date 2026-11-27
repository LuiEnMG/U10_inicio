package mx.edu.backendacademico.service;

import java.util.List;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.AlumnoNoEncontradoException;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import mx.edu.backendacademico.repository.AlumnoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlumnoService {
    private final AlumnoRepository repository;

    public AlumnoService(AlumnoRepository repository) {
        this.repository = repository;
    }

    public List<Alumno> listar() {
        return repository.buscarTodos();
    }

    public Alumno buscar(Long id) {
        return repository.buscarPorId(id).orElseThrow(() -> new AlumnoNoEncontradoException(id));
    }

    // Obtiene el expediente exclusivamente a partir del principal autenticado.
    public Alumno buscarMiExpediente(String principalName) {
        return repository.buscarTodos().stream()
                .filter(a -> a.correo().equalsIgnoreCase(principalName)
                        || a.matricula().equalsIgnoreCase(principalName))
                .findFirst()
                .orElseThrow(() -> new AlumnoNoEncontradoException(principalName));
    }

    // Regla de acceso por objeto: ADMIN puede ver cualquiera; el resto solo si es propietario.
    public Alumno buscarPorIdConControl(Long id, Authentication authentication) {
        Alumno alumno = buscar(id);
        if (esAdministrador(authentication) || esPropietario(alumno, authentication)) {
            return alumno;
        }
        throw new AccessDeniedException("No tienes permiso para consultar este expediente.");
    }

    private boolean esAdministrador(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean esPropietario(Alumno alumno, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return false;
        }
        String usuarioActual = authentication.getName();
        return usuarioActual.equalsIgnoreCase(alumno.correo())
                || usuarioActual.equalsIgnoreCase(alumno.matricula());
    }

    @Transactional
    public Alumno crear(String matricula, String nombre, String correo) {
        if (repository.existePorMatricula(matricula)) {
            throw new MatriculaDuplicadaException(matricula);
        }
        return repository.guardar(new Alumno(null, matricula, nombre, correo, EstatusAlumno.ACTIVO));
    }

    @Transactional
    public Alumno actualizar(Long id, String nombre, String correo) {
        var actual = buscar(id);
        return repository.guardar(new Alumno(id, actual.matricula(), nombre, correo, actual.estatus()));
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        repository.eliminarPorId(id);
    }
}