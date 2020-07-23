package com.service.processor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.service.annotations.AutoService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public class AutoServiceProcessor extends AbstractProcessor {

    static final String MISSING_SERVICES_ERROR = "No service interfaces provided for element!";

    private Multimap<String, String> mProviders = HashMultimap.create();

    @Override
    public ImmutableSet<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(AutoService.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return processImpl(annotations, roundEnv);
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            fatalError(writer.toString());
            return true;
        }
    }

    private boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            generateConfigFiles();
        } else {
            processAnnotations(annotations, roundEnv);
        }
        return true;
    }

    private void processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(AutoService.class);

        log(annotations.toString());
        log(elements.toString());

        for (Element e : elements) {
            TypeElement providerImplementer = (TypeElement) e;
            AnnotationMirror annotationMirror = AnnotationUtils.getAnnotationMirror(e, AutoService.class);
            Set<DeclaredType> providerInterfaces = getValueFieldOfClasses(annotationMirror);
            if (providerInterfaces.isEmpty()) {
                error(MISSING_SERVICES_ERROR, e, annotationMirror);
                continue;
            }

            for (DeclaredType providerInterface : providerInterfaces) {
                TypeElement providerType = AnnotationUtils.asTypeElement(providerInterface);

                log("provider interface: " + providerType.getQualifiedName());
                log("provider implementer: " + providerImplementer.getQualifiedName());

                if (checkImplementer(providerImplementer, providerType)) {
                    mProviders.put(getBinaryName(providerType), getBinaryName(providerImplementer));
                } else {
                    String message = "ServiceProviders must implement their service provider interface. "
                            + providerImplementer.getQualifiedName() + " does not implement "
                            + providerType.getQualifiedName();
                    error(message, e, annotationMirror);
                }
            }
        }
    }

    private void generateConfigFiles() {
        Filer filer = processingEnv.getFiler();

        for (String providerInterface : mProviders.keySet()) {
            String resourceFile = "META-INF/services/" + providerInterface;
            log("Working on resource file: " + resourceFile);
            try {
                SortedSet<String> allServices = Sets.newTreeSet();
                try {
                    FileObject existingFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
                    log("Looking for existing resource file at " + existingFile.toUri());
                    Set<String> oldServices = ServicesFiles.readServiceFile(existingFile.openInputStream());
                    log("Existing service entries: " + oldServices);
                    allServices.addAll(oldServices);
                } catch (IOException e) {
                    log("Resource file did not already exist.");
                }

                Set<String> newServices = new HashSet<String>(mProviders.get(providerInterface));
                if (allServices.containsAll(newServices)) {
                    log("No new service entries being added.");
                    return;
                }

                allServices.addAll(newServices);
                log("New service file contents: " + allServices);
                FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
                OutputStream out = fileObject.openOutputStream();
                ServicesFiles.writeServiceFile(allServices, out);
                out.close();
                log("Wrote to: " + fileObject.toUri());
            } catch (IOException e) {
                fatalError("Unable to create " + resourceFile + ", " + e);
                return;
            }
        }
    }

    private boolean checkImplementer(TypeElement providerImplementer, TypeElement providerType) {
        String verify = processingEnv.getOptions().get("verify");
        if (verify == null || !Boolean.valueOf(verify)) {
            return true;
        }

        Types types = processingEnv.getTypeUtils();
        return types.isSubtype(providerImplementer.asType(), providerType.asType());
    }

    private String getBinaryName(TypeElement element) {
        return getBinaryNameImpl(element, element.getSimpleName().toString());
    }

    private String getBinaryNameImpl(TypeElement element, String className) {
        Element enclosingElement = element.getEnclosingElement();

        if (enclosingElement instanceof PackageElement) {
            PackageElement pkg = (PackageElement) enclosingElement;
            if (pkg.isUnnamed()) {
                return className;
            }
            return pkg.getQualifiedName() + "." + className;
        }

        TypeElement typeElement = (TypeElement) enclosingElement;
        return getBinaryNameImpl(typeElement, typeElement.getSimpleName() + "$" + className);
    }

    private ImmutableSet<DeclaredType> getValueFieldOfClasses(AnnotationMirror annotationMirror) {
        return AnnotationUtils.getAnnotationValue(annotationMirror, "value")
                .accept(
                        new SimpleAnnotationValueVisitor8<ImmutableSet<DeclaredType>, Void>() {
                            @Override
                            public ImmutableSet<DeclaredType> visitType(TypeMirror typeMirror, Void v) {
                                return ImmutableSet.of(AnnotationUtils.asDeclared(typeMirror));
                            }

                            @Override
                            public ImmutableSet<DeclaredType> visitArray(List<? extends AnnotationValue> values, Void v) {
                                return values
                                        .stream()
                                        .flatMap(value -> value.accept(this, null).stream())
                                        .collect(ImmutableSet.toImmutableSet());
                            }
                        },
                        null);
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Kind.NOTE, msg);
        }
    }

    private void error(String msg, Element element, AnnotationMirror annotation) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg, element, annotation);
    }

    private void fatalError(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, "FATAL ERROR: " + msg);
    }
}
