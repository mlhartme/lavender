<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="de-DE">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>abc</title>
<meta name="description" content="abc">
<meta name="keywords" content="abc">
<meta content="abc" name="copyright">
<meta content="index,follow" name="robots">

<!-- referenced css -->
<link type="text/css" href="main.css" rel="stylesheet">
<link type="text/css" href="other.css" rel="stylesheet">

<!-- inline javascript -->
<script language="JavaScript" type="text/javascript"><!-- 
var A='abc';
	//--></script>

<!-- conditionals -->
<!--[if lt IE 7]><link type="text/css" href="ie6.css" rel="stylesheet"><![endif]-->
<!--[if IE 7]><link type="text/css" href="ie7.css" rel="stylesheet"><![endif]-->

</head>

<body>

<%
    for ( int i = 0; i < 20; i++ ) {
        %>
        <img src="logo.png" alt="abc" height="58" width="58">
        <%
    }
%>
<%
    for ( int i = 0; i < 10; i++ ) {
        %>
        <div style="background-color: rgb(255, 150, 0);url(background.png)" id="01" />
        <%
    }
%>

<!-- referenced javascript -->
<script type="text/javascript" src="main.js"></script>

<!-- chunk -->
<h1>HTML Ipsum Presents</h1>
	       
<p><strong>Pellentesque habitant morbi tristique</strong> senectus et netus et malesuada fames ac turpis egestas. Vestibulum tortor quam, feugiat vitae, ultricies eget, tempor sit amet, ante. Donec eu libero sit amet quam egestas semper. <em>Aenean ultricies mi vitae est.</em> Mauris placerat eleifend leo. Quisque sit amet est et sapien ullamcorper pharetra. Vestibulum erat wisi, condimentum sed, <code>commodo vitae</code>, ornare sit amet, wisi. Aenean fermentum, elit eget tincidunt condimentum, eros ipsum rutrum orci, sagittis tempus lacus enim ac dui. <a href="#">Donec non enim</a> in turpis pulvinar facilisis. Ut felis.</p>

<h2>Header Level 2</h2>
	       
<ol>
   <li>Lorem ipsum dolor sit amet, consectetuer adipiscing elit.</li>
   <li>Aliquam tincidunt mauris eu risus.</li>
</ol>

<blockquote><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus magna. Cras in mi at felis aliquet congue. Ut a est eget ligula molestie gravida. Curabitur massa. Donec eleifend, libero at sagittis mollis, tellus est malesuada tellus, at luctus turpis elit sit amet quam. Vivamus pretium ornare est.</p></blockquote>

<h3>Header Level 3</h3>

<ul>
   <li>Lorem ipsum dolor sit amet, consectetuer adipiscing elit.</li>
   <li>Aliquam tincidunt mauris eu risus.</li>
</ul>

<pre><code>
#header h1 a { 
	display: block; 
	width: 300px; 
	height: 80px; 
}
</code></pre>

<form action="#" method="post">
    <div>
         <label for="name">Text Input:</label>
         <input type="text" name="name" id="name" value="" tabindex="1" />
    </div>

    <div>
         <h4>Radio Button Choice</h4>

         <label for="radio-choice-1">Choice 1</label>
         <input type="radio" name="radio-choice-1" id="radio-choice-1" tabindex="2" value="choice-1" />

		 <label for="radio-choice-2">Choice 2</label>
         <input type="radio" name="radio-choice-2" id="radio-choice-2" tabindex="3" value="choice-2" />
    </div>

	<div>
		<label for="select-choice">Select Dropdown Choice:</label>
		<select name="select-choice" id="select-choice">
			<option value="Choice 1">Choice 1</option>
			<option value="Choice 2">Choice 2</option>
			<option value="Choice 3">Choice 3</option>
		</select>
	</div>
	
	<div>
		<label for="textarea">Textarea:</label>
		<textarea cols="40" rows="8" name="textarea" id="textarea"></textarea>
	</div>
	
	<div>
	    <label for="checkbox">Checkbox:</label>
		<input type="checkbox" name="checkbox" id="checkbox" />
    </div>

	<div>
	    <input type="submit" value="Submit" />
    </div>
</form>

<ul>
   <li>Morbi in sem quis dui placerat ornare. Pellentesque odio nisi, euismod in, pharetra a, ultricies in, diam. Sed arcu. Cras consequat.</li>
   <li>Praesent dapibus, neque id cursus faucibus, tortor neque egestas augue, eu vulputate magna eros eu erat. Aliquam erat volutpat. Nam dui mi, tincidunt quis, accumsan porttitor, facilisis luctus, metus.</li>
   <li>Phasellus ultrices nulla quis nibh. Quisque a lectus. Donec consectetuer ligula vulputate sem tristique cursus. Nam nulla quam, gravida non, commodo a, sodales sit amet, nisi.</li>
   <li>Pellentesque fermentum dolor. Aliquam quam lectus, facilisis auctor, ultrices ut, elementum vulputate, nunc.</li>
</ul>

</body>
</html>