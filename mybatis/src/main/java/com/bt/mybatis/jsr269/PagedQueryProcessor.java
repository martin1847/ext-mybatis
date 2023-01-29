///**
// * Botaoyx.com Inc.
// * Copyright (c) 2021-2023 All Rights Reserved.
// */
//package com.bt.mybatis.jsr269;
//
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import javax.annotation.processing.AbstractProcessor;
//import javax.annotation.processing.RoundEnvironment;
//import javax.annotation.processing.SupportedAnnotationTypes;
//import javax.lang.model.SourceVersion;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.ElementKind;
//import javax.lang.model.element.ExecutableElement;
//import javax.lang.model.element.TypeElement;
//import javax.lang.model.element.VariableElement;
//import javax.tools.JavaFileObject;
//import javax.tools.StandardLocation;
//
//import com.bt.model.PagedQuery;
//import com.bt.rpc.annotation.RpcService;
//
///**
// *
// * @author Martin.C
// * @version 2023/01/06 17:09
// */
//@SupportedAnnotationTypes("com.bt.rpc.annotation.RpcService")
//public class PagedQueryProcessor extends AbstractProcessor {
//
//    @Override
//    public SourceVersion getSupportedSourceVersion() {
//        return SourceVersion.latestSupported();
//    }
//
//    @Override
//    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//
//        boolean modif = false;
//        for (TypeElement typeElement : annotations) {
//            // Elements to be processed PagedQuery.class
//            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(typeElement);
//
//
//            //List<Element> setters = annotatedMethods.get(true);
//            //List<Element> otherMethods = annotatedMethods.get(false);
//
//
//
//
//
//            for (var e : annotatedElements) {
//                ExecutableElement it;
//                for( var m:e.getEnclosedElements()){
//                    if(m.getKind() == ElementKind.METHOD && (it = (ExecutableElement)m).getParameters().size() == 1){
//                        VariableElement param = it.getParameters().get(0);
//                        if(param.getSimpleName().toString().equals(PagedQuery.class.getSimpleName())){
//                            log("found method with param :",param);
//                        }
//                    }
//                }
//                //var fields = e.getEnclosedElements()
//                //        .stream().filter(it ->
//                //                it.getKind() == ElementKind.METHOD
//                //        && ((ExecutableElement)it).getParameters().size() == 1
//                //        &&
//                //        )
//                //        .map(it->{
//                //            var m = (ExecutableElement)it;
//                //            m.getParameters() == 1 && m.getParameters().get(0)
//                //        })
//                //        .collect(Collectors.toList());
//
//                //fields.forEach(element -> log("found Field:",element));
//
//
//            }
//
//        }
//
//        return modif;
//
//    }
//
//
//     void testWrite(){
//        try {
//            //var file = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT,"",
//            //        e.getSimpleName().toString().replace('.','/'));
//            //
//            //log("found file :" + file.toUri());
//
//            var className = "";// e.getSimpleName().toString();
//            String packageName = null;
//            int lastDot = className.lastIndexOf('.');
//            if (lastDot > 0) {
//                packageName = className.substring(0, lastDot);
//            }
//
//            String simpleClassName = className.substring(lastDot + 1);
//            String builderClassName = "Paged"+simpleClassName;
//            String builderSimpleClassName = builderClassName
//                    .substring(lastDot + 1);
//
//            JavaFileObject builderFile = processingEnv.getFiler()
//                    .createSourceFile(builderClassName);
//
//
//            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
//                //out.println("// Test PrintWriter to File "+file.toUri());
//                if (packageName != null) {
//                    out.print("package ");
//                    out.print(packageName);
//                    out.println(";");
//                    out.println();
//                }
//
//                out.print("public class ");
//                out.print(builderSimpleClassName);
//                out.println(" {");
//                out.println();
//                //out.print("    private ");
//                //out.print(simpleClassName);
//                //out.print(" object = new ");
//                //out.print(simpleClassName);
//                //out.println("();");
//                out.println();
//                out.println("    }");
//                out.println();
//            }
//
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//
//        //var te = (TypeElement) e;
//        //var fields = te.getEnclosedElements()
//        //        .stream().filter(it -> it.getKind() == ElementKind.FIELD)
//        //        .collect(Collectors.toList());
//        //
//        //fields.forEach(element -> log("found Field:",element));
//
//    }
//
//    //private Name toGetterName(Name name) {
//    //    String s = name.toString();
//    //    return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
//    //}
//
//
//    private void log(String msg) {
//        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, msg);
//    }
//
//    private void log(String msg, Element element) {
//        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, msg, element);
//    }
//    private void warning(String msg, Element element, javax.lang.model.element.AnnotationMirror annotation) {
//        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.WARNING, msg, element, annotation);
//    }
//
//    private void error(String msg, Element element, javax.lang.model.element.AnnotationMirror annotation) {
//        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR, msg, element, annotation);
//    }
//
//    private void fatalError(String msg) {
//        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR, "FATAL ERROR: " + msg);
//    }
//}