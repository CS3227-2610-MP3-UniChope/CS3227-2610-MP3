package unichope.platform.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import unichope.platform.core.model.module.Module;
import unichope.platform.core.model.module.TutorModule;

/** Shared reads only; state changes belong to guarded management/lifecycle operations. */
public interface ModuleRepository {
    Optional<Module> findById(UUID id);
    List<Module> findAll();
    Optional<Module> findByCode(String code);
    List<TutorModule> findAssignmentsByTutor(UUID tutorId);
    List<TutorModule> findAssignments();
}
