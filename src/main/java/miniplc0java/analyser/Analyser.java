package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    Zone Standard;





    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     *
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     *
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void declareSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }


    private void analyseProgram() throws CompileError {
        Standard=new Zone();
        Standard.level_now=0;
        ArrayList<Element> BASE=new ArrayList();
        Standard.SYM.add(BASE);

        if(nextIf(TokenType.EOF)!=null)
        {
            System.out.println("好的，你是个空函数");
            return ;
        }
        var sign=peek();
        while(sign.getTokenType()==TokenType.CONST_KW||sign.getTokenType()==TokenType.LET_KW||sign.getTokenType()==TokenType.FN_KW)
        {
            if(sign.getTokenType()==TokenType.FN_KW)
                analyseFunction();
            else if(sign.getTokenType()==TokenType.LET_KW)
                analyseLetdeclstmt();
            else
                analyseConstdeclstmt();

            sign=peek();
        }

        expect(TokenType.EOF);
    }

    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);

        var Funcname=expect(TokenType.IDENT);

        Element tmp=new Element();
        tmp.isConst=false;
        tmp.isGlobal=true;
        tmp.name=Funcname.getValueString();
        Standard.SYM.get(0).add(tmp);//第0层，放函数和全局变量
        System.out.println(tmp+"当前层数"+Standard.level_now);

        Standard.level_now++;
        ArrayList<Element> Nextlevel=new ArrayList();
        Standard.SYM.add(Nextlevel);


        expect(TokenType.L_PAREN);

        if(peek().getTokenType()!=TokenType.R_PAREN)
        {
            analyseFunctionparamlist();
        }
            expect(TokenType.R_PAREN);
            expect(TokenType.ARROW);

        var Functype=expect(TokenType.IDENT);
        tmp.type=Functype.getValueString();


            analyseBlockstmt();
    }


    //function_param -> 'const'? IDENT ':' ty
    //function_param_list -> function_param (',' function_param)*
    private void analyseFunctionparamlist() throws CompileError
    {
        analyseFunctionparam();
        while (check(TokenType.COMMA))
        {
            expect(TokenType.COMMA);
            analyseFunctionparam();
        }
    }

    private void analyseFunctionparam() throws CompileError{

        int sign=0;//是不是Const？

        if(check(TokenType.CONST_KW))
        {
            sign=1;
            expect(TokenType.CONST_KW);
        }

        var tmp=expect(TokenType.IDENT);//参数名字
        expect(TokenType.COLON);
        var tmp2=expect(TokenType.IDENT);//参数类型

        Element para=new Element();
        if(sign==1)
            para.isConst=true;
        para.isPara=true;
        para.isGlobal=false;
        para.type=tmp2.getValueString();
        para.name=tmp.getValueString();
        System.out.println(para+"当前层数"+Standard.level_now);
        Standard.SYM.get(Standard.level_now).add(para);
    }


    private void analyseBlockstmt() throws CompileError
    {
        expect(TokenType.L_BRACE);

        var sign=peek();
        if(sign.getTokenType()==TokenType.R_BRACE)
        {
            expect(TokenType.R_BRACE);
            return;
        }
        else{
            while(sign.getTokenType()==TokenType.MINUS||sign.getTokenType()==TokenType.IDENT||
                    sign.getTokenType()==TokenType.UINT_LITERAL||sign.getTokenType()==TokenType.CHAR_LITERAL||
                    sign.getTokenType()==TokenType.STRING_LITERAL||sign.getTokenType()==TokenType.DOUBLE_LITERAL||
                    sign.getTokenType()==TokenType.L_PAREN||sign.getTokenType()==TokenType.LET_KW||
                    sign.getTokenType()==TokenType.IF_KW||sign.getTokenType()==TokenType.WHILE_KW||
                    sign.getTokenType()==TokenType.BREAK_KW||sign.getTokenType()==TokenType.CONTINUE_KW||
                    sign.getTokenType()==TokenType.CONST_KW||sign.getTokenType()==TokenType.RETURN_KW||
                    sign.getTokenType()==TokenType.SEMICOLON||sign.getTokenType()==TokenType.L_BRACE)
            {
                analyseStmt();
                sign=peek();
            }
            expect(TokenType.R_BRACE);
        }
    }

    private void analyseStmt() throws CompileError
    {
        var sign=peek();
        if(sign.getTokenType()==TokenType.MINUS||sign.getTokenType()==TokenType.IDENT||
                sign.getTokenType()==TokenType.UINT_LITERAL||sign.getTokenType()==TokenType.CHAR_LITERAL||
                sign.getTokenType()==TokenType.STRING_LITERAL||sign.getTokenType()==TokenType.DOUBLE_LITERAL||
                sign.getTokenType()==TokenType.L_PAREN)
            analyseExprstmt();

        else if(sign.getTokenType()==TokenType.LET_KW)
            analyseLetdeclstmt();

        else if(sign.getTokenType()==TokenType.CONST_KW)
            analyseConstdeclstmt();

        else if(sign.getTokenType()==TokenType.IF_KW)
            analyseIfstmt();

        else if(sign.getTokenType()==TokenType.WHILE_KW)
            analyseWhilestmt();

        else if(sign.getTokenType()==TokenType.BREAK_KW)
            analyseBreakstmt();

        else if(sign.getTokenType()==TokenType.CONTINUE_KW)
            analyseContinuestmt();

        else if(sign.getTokenType()==TokenType.RETURN_KW)
            analyseReturnstmt();

        else if(sign.getTokenType()==TokenType.L_BRACE)
            analyseBlockstmt();

        else if(sign.getTokenType()==TokenType.SEMICOLON)
            analyseEmptystmt();
    }

    private void analyseExprstmt() throws CompileError{
        analyseExpr();
        expect(TokenType.SEMICOLON);
    }


    private void analyseIfstmt() throws CompileError{
        expect(TokenType.IF_KW);
        analyseExpr();
        analyseBlockstmt();
        while(check(TokenType.ELSE_KW))
        {
            expect(TokenType.ELSE_KW);
            if(check(TokenType.IF_KW))
            {
                expect(TokenType.IF_KW);
                analyseExpr();
                analyseBlockstmt();
            }
            else
            {
                    analyseBlockstmt();
                    break;
            }

        }
    }


    private void analyseReturnstmt() throws CompileError{
        expect(TokenType.RETURN_KW);

        if(!check(TokenType.SEMICOLON))
        {
            analyseExpr();
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseEmptystmt() throws CompileError{expect(TokenType.SEMICOLON);}
    private void analyseContinuestmt() throws CompileError {expect(TokenType.CONTINUE_KW);expect(TokenType.SEMICOLON);}
    private void analyseBreakstmt() throws CompileError {expect(TokenType.BREAK_KW);expect(TokenType.SEMICOLON);}

    private void analyseWhilestmt() throws CompileError{
        expect(TokenType.WHILE_KW);
        analyseExpr();
        analyseBlockstmt();
    }

    private void analyseLetdeclstmt() throws CompileError{
        //let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
        expect(TokenType.LET_KW);
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        expect(TokenType.IDENT);

        var tmp=peek();
        if(tmp.getTokenType()==TokenType.ASSIGN)
        {
            expect(TokenType.ASSIGN);
            analyseExpr();
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseConstdeclstmt() throws CompileError{
        //const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
        expect(TokenType.CONST_KW);
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        expect(TokenType.IDENT);
        expect(TokenType.ASSIGN);
        analyseExpr();
        expect(TokenType.SEMICOLON);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void analyseExpr() throws CompileError{
        var tmp=peek();
        if(tmp.getTokenType()==TokenType.MINUS)//negate_expr -> '-' expr
        {
            analyseNegateexpr();
        }
        else if(tmp.getTokenType()==TokenType.IDENT)
            //assign_expr -> l_expr '=' expr  lexpr就是个IDENT
            //call_expr -> IDENT '(' call_param_list? ')'
            //ident_expr -> IDENT
        {
            var keypoint=expect(TokenType.IDENT);

            var tmp2=peek();
            if(tmp2.getTokenType()==TokenType.ASSIGN)
                analyseAssignexpr();
            else if(tmp2.getTokenType()==TokenType.L_PAREN)
                analyseCallexpr();
            else
                {

                }//说明他就是一个identexpr

        }
        //literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL
        else if(tmp.getTokenType()==TokenType.UINT_LITERAL||tmp.getTokenType()==TokenType.CHAR_LITERAL||
                tmp.getTokenType()==TokenType.DOUBLE_LITERAL||tmp.getTokenType()==TokenType.STRING_LITERAL)
        {
            analyseLiteralexpr();
        }
        //group_expr -> '(' expr ')'
        else if(tmp.getTokenType()==TokenType.L_PAREN)
        {
            analyseGroupexpr();
        }
        else
            throw new Error("你这个Expr少了前面的零部件啊."+tmp.getTokenType()+tmp.getStartPos()+next()+next());

        analyseNEW();
    }

    //binary_operator -> '+' | '-' | '*' | '/' | '==' | '!=' | '<' | '>' | '<=' | '>='
    //NEWE -> 	(binary_operator expr | ‘as’ ty)   NEWE	| NULL(空)
    private void analyseNEW() throws CompileError
    {
        if(check(TokenType.AS_KW))
        {
            expect(TokenType.AS_KW);
            expect(TokenType.IDENT);
            analyseNEW();
        }
        else if(check(TokenType.PLUS)||check(TokenType.MINUS)||check(TokenType.MUL)||check(TokenType.DIV)||
                check(TokenType.EQ)||check(TokenType.NEQ)||check(TokenType.LT)||check(TokenType.GT)||
                check(TokenType.LE)||check(TokenType.GE))
        {
            next();
            analyseExpr();
            analyseNEW();
        }
        else {}
    }

///////////这里面的三个都是少了开头的IDENT的！！！！！

    //assign_expr -> l_expr '=' expr  lexpr就是个IDENT
    //call_expr -> IDENT '(' call_param_list? ')'
    private void analyseAssignexpr() throws CompileError{
            expect(TokenType.ASSIGN);
            analyseExpr();
    }

    private void analyseCallexpr() throws CompileError{

        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN))
        {
            analyseCallparamlist();
        }
        expect(TokenType.R_PAREN);
    }
///////////这里面的三个都是少了开头的IDENT的！！！！！

    //call_param_list -> expr (',' expr)*
    private void analyseCallparamlist() throws CompileError{
        analyseExpr();
        while(check(TokenType.COMMA))
        {
            expect(TokenType.COMMA);
            analyseExpr();
        }
    }


    //negate_expr -> '-' expr
    private void analyseNegateexpr() throws CompileError{
        expect(TokenType.MINUS);
        analyseExpr();
    }


    //literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL
    private void analyseLiteralexpr() throws CompileError{
        next();
    }

    //group_expr -> '(' expr ')'
    private void analyseGroupexpr() throws CompileError{
        expect(TokenType.L_PAREN);
        analyseExpr();
        expect(TokenType.R_PAREN);
    }










    private void analyseMain() throws CompileError {
        analyseConstantDeclaration();
        analyseVariableDeclaration();
        analyseStatementSequence();
    }

    private void analyseConstantDeclaration() throws CompileError {
        // 示例函数，示例如何解析常量声明
        // 如果下一个 token 是 const 就继续
        while (nextIf(TokenType.Const) != null) {
            // 变量名
            var nameToken = expect(TokenType.Ident);
            instructions.add(new Instruction(Operation.LIT, 0));
            addSymbol(nameToken.getValueString(),false,true,nameToken.getStartPos());


            // 等于号
            expect(TokenType.Equal);

            // 常表达式
            analyseConstantExpression();

            int bias=getOffset(nameToken.getValueString(),nameToken.getStartPos());
            instructions.add(new Instruction(Operation.STO,bias));

            declareSymbol(nameToken.getValueString(),nameToken.getStartPos());
            // 分号
            expect(TokenType.Semicolon);
        }
    }

    private void analyseVariableDeclaration() throws CompileError {
        while(nextIf(TokenType.Var)!=null)
        {
            var wor=expect(TokenType.Ident);

            instructions.add(new Instruction(Operation.LIT, 0));

            addSymbol(wor.getValueString(),false,false,wor.getStartPos());

            if(check(TokenType.Equal))
            {
                next();
                analyseExpression();
                int bias=getOffset(wor.getValueString(),wor.getStartPos());
                instructions.add(new Instruction(Operation.STO,bias));

                declareSymbol(wor.getValueString(),wor.getStartPos());
            }
            expect(TokenType.Semicolon);
        }
    }

    private void analyseStatementSequence() throws CompileError {
        analyseStatement();
    }

    private void analyseStatement() throws CompileError {
        while(true)
        {
            if(check(TokenType.Semicolon))
            {
                next();//空的
            }
            else if(check(TokenType.Print))
            {
                analyseOutputStatement();//输出语句
            }
            else if(check(TokenType.Ident)) {
                analyseAssignmentStatement();//赋值语句
            }
            else break;
        }
    }

    private void analyseConstantExpression() throws CompileError {
        int sig=0;
        if(check(TokenType.Plus))
        {
            next();
        }
        else if(check(TokenType.Minus))
        {
            next();
            sig=1;
        }

        if(check(TokenType.Uint))
        {
            instructions.add(new Instruction(Operation.LIT, 0));

            instructions.add(new Instruction(Operation.LIT, (Integer)next().getValue()));
            if(sig==1)
            {
                instructions.add(new Instruction(Operation.SUB));
            }
            else
                instructions.add(new Instruction(Operation.ADD));
        }




    }

    private void analyseExpression() throws CompileError {
        analyseItem();
        while(true)
        {
            if(check(TokenType.Plus))
            {
                next();
                analyseItem();
                instructions.add(new Instruction(Operation.ADD));
            }
            else if(check(TokenType.Minus))
            {
                next();
                analyseItem();
                instructions.add(new Instruction(Operation.SUB));
            }
            else
                break;
        }
    }

    private void analyseAssignmentStatement() throws CompileError {
        Token x=expect(TokenType.Ident);
        if(x!=null)
        {
            expect(TokenType.Equal);
            analyseExpression();
            int bias=getOffset(x.getValueString(),x.getStartPos());//看看是否定义过
            if(isConstant(x.getValueString(),x.getStartPos()))
                throw new AnalyzeError(ErrorCode.AssignToConstant, x.getStartPos());//常量不能再赋值了
            instructions.add(new Instruction(Operation.STO,bias));
            expect(TokenType.Semicolon);
        }
    }

    private void analyseOutputStatement() throws CompileError {
        expect(TokenType.Print);
        expect(TokenType.LParen);
        analyseExpression();
        expect(TokenType.RParen);
        expect(TokenType.Semicolon);
        instructions.add(new Instruction(Operation.WRT));
    }

    private void analyseItem() throws CompileError {
        analyseFactor();
        while(true)
        {
            if(check(TokenType.Div)||check(TokenType.Mult))
            {
                if(check(TokenType.Div))
                {
                    next();
                    analyseFactor();
                    instructions.add(new Instruction(Operation.DIV));
                }
                else{
                    next();
                    analyseFactor();
                    instructions.add(new Instruction(Operation.MUL));
                }
            }
            else break;
        }
    }

    private void analyseFactor() throws CompileError {
        boolean negate;
        if (nextIf(TokenType.Minus) != null) {
            negate = true;
            // 计算结果需要被 0 减
            instructions.add(new Instruction(Operation.LIT, 0));
        } else {
            nextIf(TokenType.Plus);
            negate = false;
        }

        if (check(TokenType.Ident)){
            // 调用相应的处理函数
            Token x=nextIf(TokenType.Ident);
            //查看是否定义过；
            int bias=getOffset(x.getValueString(),x.getStartPos());
            //查看是否付了值
            var entry = this.symbolTable.get(x.getValueString());
            if(entry.isInitialized)
            {
                instructions.add(new Instruction(Operation.LOD,bias));
            }
            else
            {
                throw new AnalyzeError(ErrorCode.NotInitialized, x.getStartPos());
            }

        } else if (check(TokenType.Uint)) {
            // 调用相应的处理函数
            Token x=nextIf(TokenType.Uint);
            instructions.add(new Instruction(Operation.LIT, (Integer)x.getValue()));

        } else if (check(TokenType.LParen)) {
            // 调用相应的处理函数
            nextIf(TokenType.LParen);

//            instructions.add(new Instruction(Operation.LIT, 0));

            analyseExpression();

            expect(TokenType.RParen);
        } else {
            // 都不是，摸了
            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
        }

        if (negate) {
            instructions.add(new Instruction(Operation.SUB));
    }
    }
}
