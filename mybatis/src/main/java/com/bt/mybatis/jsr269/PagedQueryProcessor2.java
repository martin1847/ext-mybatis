///**
// * Botaoyx.com Inc.
// * Copyright (c) 2021-2023 All Rights Reserved.
// */
//package com.bt.mybatis.jsr269;
//
//import java.util.HashMap;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import javax.annotation.processing.AbstractProcessor;
//import javax.annotation.processing.ProcessingEnvironment;
//import javax.annotation.processing.RoundEnvironment;
//import javax.annotation.processing.SupportedAnnotationTypes;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.ElementKind;
//import javax.lang.model.element.TypeElement;
//
//import com.sun.source.tree.Tree.Kind;
//import com.sun.tools.javac.api.JavacTrees;
//import com.sun.tools.javac.code.Flags;
//import com.sun.tools.javac.processing.JavacProcessingEnvironment;
//import com.sun.tools.javac.tree.JCTree;
//import com.sun.tools.javac.tree.JCTree.JCClassDecl;
//import com.sun.tools.javac.tree.JCTree.JCExpression;
//import com.sun.tools.javac.tree.TreeMaker;
//import com.sun.tools.javac.tree.TreeTranslator;
//import com.sun.tools.javac.util.Context;
//import com.sun.tools.javac.util.List;
//import com.sun.tools.javac.util.ListBuffer;
//import com.sun.tools.javac.util.Name;
//import com.sun.tools.javac.util.Names;
//
///**
// *
// * @author Martin.C
// * @version 2023/01/06 17:09
// */
//@SupportedAnnotationTypes("com.bt.mybatis.Paged")
//public class PagedQueryProcessor2 extends AbstractProcessor {
//
//    /**
//     * 描述语法树的实例类
//     */
//    private JavacTrees javacTrees;
//
//    /**
//     * 创建语法树节点的工具类
//     */
//    private TreeMaker treeMaker;
//
//    /**
//     * 访问语法树中的标识符
//     * eg: names.fromString("str")
//     */
//    private Names names;
//
//    /**
//     * 从AST上下文中初始化JavacTrees,TreeMaker与Names
//     */
//    @Override
//    public synchronized void init(ProcessingEnvironment processingEnv) {
//        log("----------------------ProcessingEnvironment Init: ----------------------"+ processingEnv);
//        super.init(processingEnv);
//        Context context = ((JavacProcessingEnvironment)processingEnv).getContext();
//
//        if(2>1)
//        throw new RuntimeException("xxxxxx");
//
//
//        javacTrees = JavacTrees.instance(processingEnv);
//
//        treeMaker = TreeMaker.instance(context);
//
//        names = Names.instance(context);
//
//    }
//
//
//    @Override
//    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//
//        boolean modif = false;
//        for (TypeElement typeElement : annotations) {
//            // Elements to be processed PagedQuery.class
//            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(typeElement);
//
//            //Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream().collect(
//            //        Collectors.partitioningBy(element ->
//            //                ((ExecutableType) element.asType()).getParameterTypes().size() == 1
//            //                        && element.getSimpleName().toString().startsWith("set")));
//            //
//            //List<Element> setters = annotatedMethods.get(true);
//            //List<Element> otherMethods = annotatedMethods.get(false);
//            //
//
//            for (var e : annotatedElements) {
//                var te = (TypeElement) e;
//                var fields = te.getEnclosedElements()
//                        .stream().filter(it -> it.getKind() == ElementKind.FIELD)
//                        .map(Element::getSimpleName).collect(Collectors.toList());
//
//                System.out.println("fields : " + fields);
//
//                //获取标注了MyData注解的类的语法树
//                JCTree tree = javacTrees.getTree(e);
//
//                //processingEnv.getFiler().createSourceFile()
//
//                //notice: 解决编译错误【java.lang.AssertionError: Value of x -1】
//                // 因为treeMaker.pos的值是不会变的(=-1), 所以在遍历是需要实时更新
//                treeMaker.pos = tree.pos;
//
//                //遍历语法树(在遇到visitClassDef事件, 也就是访问到类定义节点时去修改语法树节点)
//                tree.accept(new TreeTranslator() {
//                    @Override
//                    public void visitClassDef(JCClassDecl jcClassDecl) {
//
//                        var fVars = jcClassDecl.defs.stream()
//                                //过滤出变量类型的元素
//                                .filter(o -> o.getKind().equals(Kind.VARIABLE))
//                                //强制转换元素为变量类型元素
//                                .map(o -> ((JCTree.JCVariableDecl)o)).collect(Collectors.toList());
//
//                        jcClassDecl.defs = jcClassDecl.defs.prepend(makeAsMapMethodDecl(fVars));
//
//                        //修改类节点完毕
//                        super.visitClassDef(jcClassDecl);
//                    }
//                });
//
//                modif = true;
//            }
//
//        }
//
//        return modif;
//
//    }
//
//
//
//    private Name toGetterName(Name name) {
//        String s = name.toString();
//        return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
//    }
//
//
//    private JCTree.JCMethodDecl makeAsMapMethodDecl(java.util.List<JCTree.JCVariableDecl> fieldList) {
//        /***
//         * JCStatement：声明语法树节点，常见的子类如下
//         * JCBlock：语句块语法树节点
//         * JCReturn：return语句语法树节点
//         * JCClassDecl：类定义语法树节点
//         * JCVariableDecl：字段/变量定义语法树节点
//         * JCMethodDecl：方法定义语法树节点
//         * JCModifiers：访问标志语法树节点
//         * JCExpression：表达式语法树节点，常见的子类如下
//         * JCAssign：赋值语句语法树节点
//         * JCIdent：标识符语法树节点，可以是变量，类型，关键字等等
//         */
//        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
//
//
//
//        statements.append(treeMaker.Exec(
//                treeMaker.Assign(
//                    treeMaker.Ident(names.fromString("map")),
//                    treeMaker.NewClass(
//                            null, //尚不清楚含义
//                            List.nil(), //泛型参数列表
//                            treeMaker.Ident(names.fromString(HashMap.class.getName())), //创建的类名
//                            List.nil(), //参数列表
//                            null //类定义，估计是用于创建匿名内部类
//                    )
//                )
//        ));
//
//        for(var f : fieldList) {
//            statements.append(treeMaker.Exec(
//                    treeMaker.Apply(
//                            List.nil(),
//                            treeMaker.Select(
//                                    treeMaker.Ident(names.fromString("map")),
//                                    names.fromString("put")
//                            ),
//                            List.of(treeMaker.Ident(f.getName()),callGetter(f.getName()))
//                    )
//            ));
//        }
//
//
//        statements.append(treeMaker.Return(treeMaker.Ident(names.fromString("map"))));
//
//        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
//
//
//        //生成返回类型标识 void
//        JCExpression returnMethodType = treeMaker.Ident(names.fromString(HashMap.class.getName()));
//
//
//        return treeMaker.MethodDef(
//                treeMaker.Modifiers(Flags.PUBLIC),//mods：访问标志
//                names.fromString("asMap"),//name：方法名
//                returnMethodType,//restype：返回类型
//                List.nil(),//typarams：泛型参数列表
//                List.nil(),//params：参数列表
//                List.nil(),//thrown：异常声明列表
//                body,//方法体
//                null);
//    }
//
//
//    JCExpression callGetter(Name name){
//        return treeMaker.Apply(
//                List.nil(),
//                treeMaker.Select(
//                        treeMaker.Ident(names.fromString("this")),
//                        toGetterName(name)
//                ),
//                List.nil()
//        );
//    }
//
//    private void log(String msg) {
//        if (processingEnv.getOptions().containsKey("debug")) {
//            processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, msg);
//        }
//    }
//
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